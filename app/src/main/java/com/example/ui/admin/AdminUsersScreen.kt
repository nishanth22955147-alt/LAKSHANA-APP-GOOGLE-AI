package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StripeViolet
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val users by viewModel.users.collectAsState()
    val pendingUsers by viewModel.pendingUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedTab by remember { mutableStateOf(if (pendingUsers.isNotEmpty()) 0 else 1) }
    var isCreateUserDialogOpen by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var pinResetUser by remember { mutableStateOf<UserEntity?>(null) }

    // Auto switch to pending tab if new pending registrations arrive
    LaunchedEffect(pendingUsers.size) {
        if (pendingUsers.isNotEmpty() && selectedTab == 1) {
            // Keep user on current tab unless preferred
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isCreateUserDialogOpen = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Create Staff User") },
                modifier = Modifier.testTag("admin_create_user_fab")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Admin Access Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryNavy.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "User & Role Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Admin controls: Approve self-registered staff, generate PINs, and assign roles",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs: [ Pending Approvals (X) ] | [ Active Users (Y) ]
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pending Approvals")
                            if (pendingUsers.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text("${pendingUsers.size}")
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text("Active Users (${users.filter { it.isApproved }.size})")
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Pending Approvals List
                if (pendingUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No Pending Registration Requests",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "When new staff members register with their mobile, they will appear here for Admin approval.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pendingUsers, key = { it.id }) { user ->
                            PendingUserApprovalItem(
                                user = user,
                                onApprove = { role, dept -> viewModel.approveUser(user.id, role, dept) },
                                onReject = { viewModel.rejectUser(user.id) }
                            )
                        }
                    }
                }
            } else {
                // Active Users List
                val approvedUsers = users.filter { it.isApproved }
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(approvedUsers, key = { it.id }) { user ->
                        UserRowCard(
                            user = user,
                            isCurrent = user.id == currentUser?.id,
                            onEditRole = { editingUser = user },
                            onResetPin = { pinResetUser = user },
                            onToggleActive = { viewModel.toggleUserStatus(user.id, user.isActive) }
                        )
                    }
                }
            }
        }
    }

    if (isCreateUserDialogOpen) {
        CreateUserDialog(
            onDismiss = { isCreateUserDialogOpen = false },
            onCreate = { name, mobile, role, pin, dept ->
                viewModel.createNewUserByAdmin(name, mobile, role, pin, dept)
                isCreateUserDialogOpen = false
            }
        )
    }

    editingUser?.let { user ->
        EditUserRoleDialog(
            user = user,
            onDismiss = { editingUser = null },
            onSaveRole = { newRole ->
                viewModel.updateUserRole(user.id, newRole)
                editingUser = null
            }
        )
    }

    pinResetUser?.let { user ->
        AdminResetPinDialog(
            user = user,
            onDismiss = { pinResetUser = null },
            onSaveNewPin = { newPin ->
                viewModel.adminResetUserPin(user.id, newPin) { success, msg ->
                    if (success) {
                        pinResetUser = null
                    }
                }
            }
        )
    }
}

@Composable
fun UserRowCard(
    user: UserEntity,
    isCurrent: Boolean,
    onEditRole: () -> Unit,
    onResetPin: () -> Unit,
    onToggleActive: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = when (user.role) {
                            UserRole.ADMIN -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            UserRole.MANAGER -> SecondaryTeal.copy(alpha = 0.15f)
                            UserRole.PURCHASER -> StripeViolet.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (user.role) {
                                    UserRole.ADMIN -> Icons.Default.Security
                                    UserRole.MANAGER -> Icons.Default.ManageAccounts
                                    UserRole.PURCHASER -> Icons.Default.ShoppingBag
                                    UserRole.INVENTORY_CLERK -> Icons.Default.Inventory
                                    UserRole.VIEWER -> Icons.Default.Visibility
                                },
                                contentDescription = null,
                                tint = when (user.role) {
                                    UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                                    UserRole.MANAGER -> SecondaryTeal
                                    UserRole.PURCHASER -> StripeViolet
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "YOU",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            "📱 ${user.mobileNumber} • ${user.department}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditRole) {
                        Icon(Icons.Default.EditAttributes, contentDescription = "Edit Role", tint = MaterialTheme.colorScheme.primary)
                    }
                    Switch(
                        checked = user.isActive,
                        onCheckedChange = { onToggleActive() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Role and current PIN indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Role: ${user.role.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Assigned PIN: ${user.pinHash}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onResetPin,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate / Reset PIN", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AdminResetPinDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSaveNewPin: (String) -> Unit
) {
    val context = LocalContext.current
    var newPin by remember {
        val random = (1000..9999).random().toString()
        mutableStateOf(random)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
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
                                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Generate User PIN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("For ${user.fullName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Mobile: ${user.mobileNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text("Current PIN: ${user.pinHash}", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it.filter { ch -> ch.isDigit() } },
                    label = { Text("New 4-Digit PIN for User") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            newPin = (1000..9999).random().toString()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Randomize")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("admin_new_pin_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        newPin = (1000..9999).random().toString()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Generate Random 4-Digit PIN")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("User PIN", newPin))
                            Toast.makeText(context, "PIN $newPin copied to clipboard! Share with user.", Toast.LENGTH_SHORT).show()
                            onSaveNewPin(newPin.trim())
                        },
                        enabled = newPin.length >= 4,
                        modifier = Modifier.weight(1.3f).testTag("save_user_pin_button")
                    ) {
                        Text("Assign & Copy PIN")
                    }
                }
            }
        }
    }
}

@Composable
fun EditUserRoleDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onSaveRole: (UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(user.role) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "Update Role: ${user.fullName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "📱 ${user.mobileNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                UserRole.entries.forEach { role ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedRole == role) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = { selectedRole = role }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(role.displayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                Text(role.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSaveRole(selectedRole) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Role")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, mobile: String, role: UserRole, pin: String, dept: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var dept by remember { mutableStateOf("Procurement") }
    var pin by remember { mutableStateOf((1000..9999).random().toString()) }
    var selectedRole by remember { mutableStateOf(UserRole.PURCHASER) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    "Create New Staff User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Admin assigns mobile number and login PIN for the staff member",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it.filter { ch -> ch.isDigit() }.take(12) },
                    label = { Text("Mobile Number *") },
                    placeholder = { Text("e.g. 9845012345") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text("Department") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("Login PIN *") },
                        trailingIcon = {
                            IconButton(onClick = { pin = (1000..9999).random().toString() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "New PIN")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Assign Role", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))

                UserRole.entries.take(4).forEach { role ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        RadioButton(
                            selected = selectedRole == role,
                            onClick = { selectedRole = role }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(role.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onCreate(name.trim(), mobile.trim(), selectedRole, pin.trim(), dept.trim())
                        },
                        enabled = name.isNotBlank() && mobile.length >= 8 && pin.length >= 4,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create User")
                    }
                }
            }
        }
    }
}
