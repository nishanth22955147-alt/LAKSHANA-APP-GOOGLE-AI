package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserRole
import com.example.ui.admin.AdminUsersScreen
import com.example.ui.admin.PendingApprovalsDialog
import com.example.ui.components.LakshanaLogoHeader
import com.example.ui.components.OnlineSyncDialog
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.excel.DailyExcelUploadDialog
import com.example.ui.inventory.InventoryScreen
import com.example.ui.items.ItemsScreen
import com.example.ui.purchase.PurchaseScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.stripe.StripePaymentDialog
import com.example.ui.supplier.SupplierScreen
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.transaction.TransactionScreen
import com.example.ui.website.WebsiteScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NavItem(
    val destination: NavigationDestination,
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentDestination by viewModel.currentDestination.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val syncSummary by viewModel.syncSummary.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val paymentModalState by viewModel.paymentModalState.collectAsState()
    val isAdmin = currentUser?.role == UserRole.ADMIN

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var isExcelUploadDialogOpen by remember { mutableStateOf(false) }
    var selectedSpreadsheetDateForUpload by remember { mutableStateOf("2026-09-25") }

    var isUserMenuExpanded by remember { mutableStateOf(false) }
    var isLogoutConfirmOpen by remember { mutableStateOf(false) }
    var isClearDataConfirmOpen by remember { mutableStateOf(false) }

    // Left Drawer Dialog States
    var isPinChangeDialogOpen by remember { mutableStateOf(false) }
    var isPendingApprovalsDialogOpen by remember { mutableStateOf(false) }
    var isSyncDialogOpen by remember { mutableStateOf(false) }
    var isAppDetailsDialogOpen by remember { mutableStateOf(false) }
    var isSpreadsheetDateDialogOpen by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Hardware back press behavior
    BackHandler(enabled = drawerState.isOpen || currentDestination !is NavigationDestination.Dashboard) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
        } else {
            viewModel.navigateTo(NavigationDestination.Dashboard)
        }
    }

    val navItems = remember(currentUser?.role) {
        val list = mutableListOf(
            NavItem(NavigationDestination.Dashboard, "Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
            NavItem(NavigationDestination.Items, "Items", Icons.Default.Inventory2, "nav_items"),
            NavItem(NavigationDestination.Purchase, "Purchase", Icons.Default.ShoppingBag, "nav_purchase"),
            NavItem(NavigationDestination.Supplier, "Supplier", Icons.Default.Storefront, "nav_supplier"),
            NavItem(NavigationDestination.Transaction, "Transaction", Icons.Default.ReceiptLong, "nav_transaction"),
            NavItem(NavigationDestination.Reports, "Reports", Icons.Default.Assessment, "nav_reports"),
            NavItem(NavigationDestination.Inventory, "Inventory", Icons.Default.Warehouse, "nav_inventory"),
            NavItem(NavigationDestination.Website, "Website", Icons.Default.Language, "nav_website")
        )
        if (currentUser?.role == UserRole.ADMIN) {
            list.add(NavItem(NavigationDestination.AdminUsers, "Users", Icons.Default.ManageAccounts, "nav_users"))
        }
        list
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .testTag("app_settings_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    // Drawer App Header with Full Attached Logo
                    LakshanaLogoHeader(
                        height = 88.dp,
                        showContainerCard = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // User Details Profile Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isAdmin) MaterialTheme.colorScheme.primary else SecondaryTeal,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            (currentUser?.fullName?.take(1) ?: "U").uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        currentUser?.fullName ?: "Staff Member",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "📱 ${currentUser?.mobileNumber ?: "9876543210"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAdmin) MaterialTheme.colorScheme.primaryContainer else SecondaryTeal.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        currentUser?.role?.displayName ?: "User",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAdmin) MaterialTheme.colorScheme.primary else SecondaryTeal,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    currentUser?.department ?: "Procurement",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "APP SETTINGS & FEATURES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Setting Item: Approve User Registrations (Admin only)
                    if (isAdmin) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null) },
                            label = { Text("Approve Registrations") },
                            badge = {
                                if (pendingUsers.isNotEmpty()) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("${pendingUsers.size}")
                                    }
                                }
                            },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                isPendingApprovalsDialogOpen = true
                            },
                            modifier = Modifier.testTag("drawer_approve_users")
                        )
                    }

                    // Setting Item: Security PIN Change
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Pin, contentDescription = null) },
                        label = { Text("Security PIN Change") },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            isPinChangeDialogOpen = true
                        },
                        modifier = Modifier.testTag("drawer_pin_change")
                    )

                    // Setting Item: Online Cloud Synchronize
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CloudSync, contentDescription = null, tint = SecondaryTeal) },
                        label = { Text("Online Cloud Synchronize") },
                        badge = {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (syncSummary.isOnline) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    if (syncSummary.isOnline) "Synced" else "Offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (syncSummary.isOnline) SuccessGreen else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            isSyncDialogOpen = true
                        },
                        modifier = Modifier.testTag("drawer_online_sync")
                    )

                    // Feature Item: Website Portal
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        label = { Text("Website Portal") },
                        selected = currentDestination == NavigationDestination.Website,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.navigateTo(NavigationDestination.Website)
                        },
                        modifier = Modifier.testTag("drawer_website_portal")
                    )

                    // Setting Item: App Details & Description
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("App Details & Description") },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            isAppDetailsDialogOpen = true
                        },
                        modifier = Modifier.testTag("drawer_app_details")
                    )

                    // Feature Item: Check Daily Reports
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                        label = { Text("Check Daily Reports") },
                        selected = currentDestination == NavigationDestination.Reports,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.navigateTo(NavigationDestination.Reports)
                        },
                        modifier = Modifier.testTag("drawer_daily_reports")
                    )

                    // Feature Item: Transaction Reports
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                        label = { Text("Transaction Reports") },
                        selected = currentDestination == NavigationDestination.Transaction,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            viewModel.navigateTo(NavigationDestination.Transaction)
                        },
                        modifier = Modifier.testTag("drawer_transaction_reports")
                    )

                    // Feature Item: Daily Spreadsheet Date
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        label = { Text("Daily Spreadsheet Date") },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            isSpreadsheetDateDialogOpen = true
                        },
                        modifier = Modifier.testTag("drawer_spreadsheet_date")
                    )

                    // Admin only items
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "ADMINISTRATIVE TOOLS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.ManageAccounts, contentDescription = null) },
                            label = { Text("User Role Management") },
                            selected = currentDestination == NavigationDestination.AdminUsers,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                viewModel.navigateTo(NavigationDestination.AdminUsers)
                            }
                        )

                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            label = { Text("Clear All Example Data", color = MaterialTheme.colorScheme.error) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                isClearDataConfirmOpen = true
                            },
                            modifier = Modifier.testTag("drawer_clear_example_data")
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Log Out
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        label = { Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold) },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            isLogoutConfirmOpen = true
                        },
                        modifier = Modifier.testTag("drawer_logout")
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("app_drawer_open_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open App Settings & Navigation")
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Grass,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    when (currentDestination) {
                                        NavigationDestination.Dashboard -> "Dashboard"
                                        NavigationDestination.Items -> "Item Master Catalog"
                                        NavigationDestination.Purchase -> "Purchase Orders"
                                        NavigationDestination.Supplier -> "Suppliers Directory"
                                        NavigationDestination.Transaction -> "Settlement Transactions"
                                        NavigationDestination.Reports -> "Analytics & Reports"
                                        NavigationDestination.Inventory -> "Inventory Management"
                                        NavigationDestination.AdminUsers -> "Admin User Roles"
                                        NavigationDestination.Website -> "Lakshana Web Portal"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Lakshana Veggie",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Online Cloud Synchronize Status Button
                        IconButton(
                            onClick = { isSyncDialogOpen = true },
                            modifier = Modifier.testTag("top_bar_sync_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (syncSummary.isOnline) SuccessGreen else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(8.dp)
                                    ) {}
                                }
                            ) {
                                Icon(
                                    if (isSyncing) Icons.Default.Sync else if (syncSummary.isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = "Online Synchronize",
                                    tint = if (syncSummary.isOnline) SecondaryTeal else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Quick Excel Upload button
                        IconButton(
                            onClick = { isExcelUploadDialogOpen = true },
                            modifier = Modifier.testTag("top_bar_excel_upload_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = "Daily Excel Upload", tint = SecondaryTeal)
                        }

                        // User Profile & Role button with menu
                        Box {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clickable { isUserMenuExpanded = true }
                                    .padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        currentUser?.role?.displayName ?: "User",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isUserMenuExpanded,
                                onDismissRequest = { isUserMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(currentUser?.fullName ?: "Staff", fontWeight = FontWeight.Bold)
                                            Text("📱 ${currentUser?.mobileNumber}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    },
                                    onClick = { isUserMenuExpanded = false },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Change Security PIN") },
                                    onClick = {
                                        isUserMenuExpanded = false
                                        isPinChangeDialogOpen = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("App Details & Description") },
                                    onClick = {
                                        isUserMenuExpanded = false
                                        isAppDetailsDialogOpen = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Daily Excel Bulk Upload") },
                                    onClick = {
                                        isUserMenuExpanded = false
                                        isExcelUploadDialogOpen = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) }
                                )
                                if (isAdmin) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Manage Users & Roles") },
                                        onClick = {
                                            isUserMenuExpanded = false
                                            viewModel.navigateTo(NavigationDestination.AdminUsers)
                                        },
                                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Clear All Example Data", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            isUserMenuExpanded = false
                                            isClearDataConfirmOpen = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Log Out", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        isUserMenuExpanded = false
                                        isLogoutConfirmOpen = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentDestination == item.destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(item.destination) },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = {
                                Text(
                                    item.title,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentDestination) {
                    NavigationDestination.Dashboard -> DashboardScreen(
                        viewModel = viewModel,
                        onOpenExcelUpload = { isExcelUploadDialogOpen = true }
                    )
                    NavigationDestination.Items -> ItemsScreen(viewModel = viewModel)
                    NavigationDestination.Purchase -> PurchaseScreen(
                        viewModel = viewModel,
                        onOpenExcelUpload = { isExcelUploadDialogOpen = true }
                    )
                    NavigationDestination.Supplier -> SupplierScreen(viewModel = viewModel)
                    NavigationDestination.Transaction -> TransactionScreen(viewModel = viewModel)
                    NavigationDestination.Reports -> ReportsScreen(viewModel = viewModel)
                    NavigationDestination.Inventory -> InventoryScreen(viewModel = viewModel)
                    NavigationDestination.AdminUsers -> AdminUsersScreen(viewModel = viewModel)
                    NavigationDestination.Website -> WebsiteScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Online Cloud Synchronize Hub Dialog
    OnlineSyncDialog(
        isOpen = isSyncDialogOpen,
        onDismiss = { isSyncDialogOpen = false },
        viewModel = viewModel
    )

    // Daily Excel Sheet Upload Dialog
    DailyExcelUploadDialog(
        isOpen = isExcelUploadDialogOpen,
        onDismiss = { isExcelUploadDialogOpen = false },
        viewModel = viewModel,
        initialDate = selectedSpreadsheetDateForUpload
    )

    // Stripe / Cash / UPI Payment Settlement Dialog
    StripePaymentDialog(
        state = paymentModalState,
        viewModel = viewModel
    )

    // PIN Change Dialog
    if (isPinChangeDialogOpen) {
        ChangePinDialog(
            onDismiss = { isPinChangeDialogOpen = false },
            onSubmit = { oldPin, newPin, onResult ->
                viewModel.changePin(oldPin, newPin, onResult)
            }
        )
    }

    // Pending User Approvals Dialog (Admin App Settings)
    if (isPendingApprovalsDialogOpen) {
        PendingApprovalsDialog(
            isOpen = isPendingApprovalsDialogOpen,
            onDismiss = { isPendingApprovalsDialogOpen = false },
            viewModel = viewModel
        )
    }

    // App Details & Description Dialog
    if (isAppDetailsDialogOpen) {
        AppDetailsDialog(
            onDismiss = { isAppDetailsDialogOpen = false }
        )
    }

    // Daily Spreadsheet Date Picker Dialog
    if (isSpreadsheetDateDialogOpen) {
        DailySpreadsheetDateDialog(
            initialDate = selectedSpreadsheetDateForUpload,
            onDismiss = { isSpreadsheetDateDialogOpen = false },
            onDateSelected = { chosenDate ->
                selectedSpreadsheetDateForUpload = chosenDate
                isSpreadsheetDateDialogOpen = false
                isExcelUploadDialogOpen = true
            }
        )
    }

    // Clear All Example Data Confirmation Dialog (Admin only)
    if (isClearDataConfirmOpen && isAdmin) {
        AlertDialog(
            onDismissRequest = { isClearDataConfirmOpen = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Example Data?") },
            text = { Text("This will permanently remove all example items, suppliers, purchase records, transactions, and inventory movements. You will start with a completely fresh Lakshana Veggie database.") },
            confirmButton = {
                Button(
                    onClick = {
                        isClearDataConfirmOpen = false
                        viewModel.clearAllExampleData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { isClearDataConfirmOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Log Out Confirmation Dialog
    if (isLogoutConfirmOpen) {
        AlertDialog(
            onDismissRequest = { isLogoutConfirmOpen = false },
            title = { Text("Log Out from Lakshana Veggie?") },
            text = { Text("You will need to verify your mobile number and security PIN to log back in.") },
            confirmButton = {
                Button(
                    onClick = {
                        isLogoutConfirmOpen = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { isLogoutConfirmOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSubmit: (oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Pin, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Change Security PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 6) oldPin = it },
                    label = { Text("Current 4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("current_pin_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it },
                    label = { Text("New Security PIN (at least 4 digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) confirmPin = it },
                    label = { Text("Confirm New Security PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                )

                errorMsg?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (oldPin.isBlank()) {
                                errorMsg = "Please enter your current PIN."
                                return@Button
                            }
                            if (newPin.length < 4) {
                                errorMsg = "New PIN must be at least 4 digits."
                                return@Button
                            }
                            if (newPin != confirmPin) {
                                errorMsg = "New PIN and confirmation do not match."
                                return@Button
                            }
                            isSubmitting = true
                            errorMsg = null
                            onSubmit(oldPin, newPin) { success, msg ->
                                isSubmitting = false
                                if (success) {
                                    onDismiss()
                                } else {
                                    errorMsg = msg
                                }
                            }
                        },
                        enabled = !isSubmitting && oldPin.isNotBlank() && newPin.isNotBlank() && confirmPin.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("save_new_pin_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Update PIN")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppDetailsDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SuccessGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Grass, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Lakshana Veggie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Version 1.0.4 • Android Production", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                Text("System Description", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Lakshana Veggie is a streamlined vegetable procurement, inventory tracking, and vendor contact management application designed specifically for wholesale APMC mandi buyers, farm collectors, and retail distribution centers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Core Capabilities", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureBullet(
                        icon = Icons.Default.Contacts,
                        title = "Vegetable Supplier Directory",
                        desc = "Quick-dial phone calls, direct WhatsApp messaging, email inquiries, and payment VPA tracking."
                    )
                    FeatureBullet(
                        icon = Icons.Default.TableChart,
                        title = "Daily Spreadsheet Bulk Upload",
                        desc = "Instantly ingest daily purchase sheets with date, items, boxes, quantity (kgs), rate, and automatic stock updates."
                    )
                    FeatureBullet(
                        icon = Icons.Default.CurrencyRupee,
                        title = "Indian Rupee (INR ₹) Settlements",
                        desc = "100% Indian Rupee accounting with Cash voucher generation and instant UPI settlement verification."
                    )
                    FeatureBullet(
                        icon = Icons.Default.Security,
                        title = "Role-Secured Administrative Control",
                        desc = "Administrators exclusively retain authorization to delete records and clear data; staff enjoy full view and intake privileges."
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun FeatureBullet(icon: ImageVector, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DailySpreadsheetDateDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    var chosenDate by remember { mutableStateOf(initialDate) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = SecondaryTeal.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Daily Spreadsheet Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Select the manifest date for daily vegetable spreadsheet intake:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Presets
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = chosenDate == "2026-09-25",
                        onClick = { chosenDate = "2026-09-25" },
                        label = { Text("Today (25 Sep)") }
                    )
                    FilterChip(
                        selected = chosenDate == "2026-09-24",
                        onClick = { chosenDate = "2026-09-24" },
                        label = { Text("Yesterday (24 Sep)") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = chosenDate,
                    onValueChange = { chosenDate = it },
                    label = { Text("Target Procurement Date (YYYY-MM-DD)") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("spreadsheet_date_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Required Spreadsheet Headers:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Date, Items, Boxes, Qty(kgs), Rate, Total",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onDateSelected(chosenDate.trim()) },
                        enabled = chosenDate.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        modifier = Modifier.weight(1.3f).testTag("confirm_spreadsheet_date_button")
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Upload")
                    }
                }
            }
        }
    }
}
