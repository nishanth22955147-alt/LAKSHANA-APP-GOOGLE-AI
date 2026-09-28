package com.example.ui.purchase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ItemEntity
import com.example.data.model.PurchaseEntryEntity
import com.example.data.model.PurchaseStatus
import com.example.data.model.SupplierEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.components.DateFormatterUtil
import com.example.ui.components.DateForwardBackwardSelector
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    viewModel: MainViewModel,
    onOpenExcelUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val purchases by viewModel.purchases.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val items by viewModel.items.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role == com.example.data.model.UserRole.ADMIN

    var statusFilter by remember { mutableStateOf("All") }
    var isNewPurchaseDialogOpen by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<PurchaseEntryEntity?>(null) }
    var deletingPurchase by remember { mutableStateOf<PurchaseEntryEntity?>(null) }

    val filteredPurchases = purchases.filter { p ->
        when (statusFilter) {
            "Pending" -> p.status == PurchaseStatus.PENDING_PAYMENT
            "Paid" -> p.status == PurchaseStatus.PAID || p.status == PurchaseStatus.PAID_CASH || p.status == PurchaseStatus.PAID_UPI
            else -> true
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onOpenExcelUpload,
                    containerColor = SecondaryTeal,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("excel_upload_quick_fab")
                ) {
                    Icon(Icons.Default.TableChart, contentDescription = "Daily Excel Upload")
                }
                Spacer(modifier = Modifier.height(10.dp))
                ExtendedFloatingActionButton(
                    onClick = { isNewPurchaseDialogOpen = true },
                    icon = { Icon(Icons.Default.AddShoppingCart, contentDescription = null) },
                    text = { Text("New Purchase") },
                    modifier = Modifier.testTag("new_purchase_fab")
                )
            }
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

            // Action Header with Daily Excel Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Daily Excel Sheet Bulk Upload",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryTeal
                        )
                        Text(
                            "Upload daily manifest (date, items, boxes, qty(kgs), rate, total) with automatic stock updates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onOpenExcelUpload,
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Pending", "Paid").forEach { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { statusFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Purchase Records (${filteredPurchases.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Total: ₹${String.format(Locale.US, "%,.2f", filteredPurchases.sumOf { it.totalAmount })}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredPurchases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No purchase records found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPurchases, key = { it.id }) { purchase ->
                        PurchaseOrderCard(
                            purchase = purchase,
                            isAdmin = isAdmin,
                            onEdit = { editingPurchase = purchase },
                            onDelete = { deletingPurchase = purchase },
                            onPayWithStripe = { viewModel.openStripePaymentForPurchase(purchase) }
                        )
                    }
                }
            }
        }
    }

    // Create New Purchase Dialog
    if (isNewPurchaseDialogOpen) {
        CreatePurchaseDialog(
            items = items,
            suppliers = suppliers,
            onDismiss = { isNewPurchaseDialogOpen = false },
            onCreate = { itemName, boxes, qtyKgs, rate, supplierName, supplierId, autoUpdateStock, date ->
                viewModel.createPurchaseOrder(
                    itemName = itemName,
                    boxes = boxes,
                    qtyKgs = qtyKgs,
                    rate = rate,
                    supplierName = supplierName,
                    supplierId = supplierId,
                    autoUpdateStock = autoUpdateStock,
                    date = date
                )
                isNewPurchaseDialogOpen = false
            }
        )
    }

    // Edit Purchase Dialog
    editingPurchase?.let { purchase ->
        EditPurchaseDialog(
            purchase = purchase,
            suppliers = suppliers,
            onDismiss = { editingPurchase = null },
            onSave = { updated ->
                viewModel.updatePurchaseOrder(updated)
                editingPurchase = null
            }
        )
    }

    // Delete Confirmation Dialog
    deletingPurchase?.let { purchase ->
        AlertDialog(
            onDismissRequest = { deletingPurchase = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Purchase Order?") },
            text = { Text("Are you sure you want to permanently delete purchase #${purchase.id} (${purchase.itemName} • ₹${String.format(Locale.US, "%.2f", purchase.totalAmount)})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePurchaseOrder(purchase)
                        deletingPurchase = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPurchase = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PurchaseOrderCard(
    purchase: PurchaseEntryEntity,
    isAdmin: Boolean = true,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPayWithStripe: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: PO Number / Batch & Status Pill with Edit/Delete Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        purchase.batchId.ifEmpty { "PO-#${purchase.id}" },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (purchase.isExcelImport) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SecondaryTeal.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "DAILY EXCEL",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryTeal,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (purchase.status) {
                            PurchaseStatus.PAID, PurchaseStatus.PAID_CASH, PurchaseStatus.PAID_UPI -> SuccessGreen.copy(alpha = 0.15f)
                            PurchaseStatus.PENDING_PAYMENT -> WarningAmber.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            purchase.status.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when (purchase.status) {
                                PurchaseStatus.PAID, PurchaseStatus.PAID_CASH, PurchaseStatus.PAID_UPI -> SuccessGreen
                                PurchaseStatus.PENDING_PAYMENT -> WarningAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit PO", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    if (isAdmin) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete PO", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Item Name & Supplier
            Text(
                purchase.itemName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val cal = remember(purchase.date) { DateFormatterUtil.parseToCalendar(purchase.date) }
            val formattedDate = DateFormatterUtil.formatDDMMYY(cal)
            val compactDate = DateFormatterUtil.formatCompactDDMMYY(cal)
            val relLabel = DateFormatterUtil.getRelativeTag(cal)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Date: $formattedDate",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        "DDMMYY: $compactDate",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                if (relLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("($relLabel)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Supplier: ${purchase.supplierName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(10.dp))

            // Metrics: Boxes, Qty(kgs), Rate, Total = qty(kgs) * rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Boxes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${purchase.boxes} boxes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Qty (kgs)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${purchase.qtyKgs} kgs", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.US, "%.2f", purchase.rate)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total (qty*rate)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "₹${String.format(Locale.US, "%.2f", purchase.totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Pay button if pending (Cash or UPI only)
            if (purchase.status == PurchaseStatus.PENDING_PAYMENT) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onPayWithStripe,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("pay_cash_upi_button")
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settle Payment (Cash / UPI) • ₹${String.format(Locale.US, "%.2f", purchase.totalAmount)}")
                }
            } else if (purchase.stripePaymentIntentId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Settlement Ref: ${purchase.stripePaymentIntentId}",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseDialog(
    items: List<ItemEntity>,
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onCreate: (itemName: String, boxes: Int, qtyKgs: Double, rate: Double, supplierName: String, supplierId: Long?, autoUpdateStock: Boolean, date: String) -> Unit
) {
    var itemName by remember { mutableStateOf(items.firstOrNull()?.name ?: "") }
    var boxesText by remember { mutableStateOf("10") }
    var qtyKgsText by remember { mutableStateOf("200.0") }
    var rateText by remember { mutableStateOf(items.firstOrNull()?.defaultRatePerKg?.toString() ?: "40.0") }
    var supplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "Lakshana Farm Suppliers") }
    var purchaseDate by remember { mutableStateOf(DateFormatterUtil.todayDDMMYY()) }
    var autoUpdateStock by remember { mutableStateOf(true) }

    val boxes = boxesText.toIntOrNull() ?: 0
    val qty = qtyKgsText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val total = Math.round(qty * rate * 100.0) / 100.0

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
                    "Create New Purchase Order",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Date Forward & Backward Selector (DDMMYY)
                DateForwardBackwardSelector(
                    selectedDateStr = purchaseDate,
                    onDateChange = { purchaseDate = it },
                    label = "Purchase Date (DDMMYY)",
                    testTagPrefix = "new_po_date"
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Vendor / Supplier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = boxesText,
                        onValueChange = { boxesText = it },
                        label = { Text("Boxes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = qtyKgsText,
                        onValueChange = { qtyKgsText = it },
                        label = { Text("Qty (kgs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate per Kg (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calculated Total (qty * rate):", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "₹${String.format(Locale.US, "%.2f", total)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoUpdateStock, onCheckedChange = { autoUpdateStock = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-update inventory stock on save", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val sup = suppliers.firstOrNull { it.name.equals(supplierName, ignoreCase = true) }
                            onCreate(itemName.trim(), boxes, qty, rate, supplierName.trim(), sup?.id, autoUpdateStock, purchaseDate)
                        },
                        enabled = itemName.isNotBlank() && qty > 0.0 && rate > 0.0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create PO")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPurchaseDialog(
    purchase: PurchaseEntryEntity,
    suppliers: List<SupplierEntity>,
    onDismiss: () -> Unit,
    onSave: (PurchaseEntryEntity) -> Unit
) {
    var itemName by remember { mutableStateOf(purchase.itemName) }
    var boxesText by remember { mutableStateOf(purchase.boxes.toString()) }
    var qtyKgsText by remember { mutableStateOf(purchase.qtyKgs.toString()) }
    var rateText by remember { mutableStateOf(purchase.rate.toString()) }
    var supplierName by remember { mutableStateOf(purchase.supplierName) }
    var dateText by remember { mutableStateOf(purchase.date) }
    var selectedStatus by remember { mutableStateOf(purchase.status) }

    val boxes = boxesText.toIntOrNull() ?: 0
    val qty = qtyKgsText.toDoubleOrNull() ?: 0.0
    val rate = rateText.toDoubleOrNull() ?: 0.0
    val total = Math.round(qty * rate * 100.0) / 100.0

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
                    "Edit Purchase Order #${purchase.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Date Forward & Backward Selector (DDMMYY)
                DateForwardBackwardSelector(
                    selectedDateStr = dateText,
                    onDateChange = { dateText = it },
                    label = "Purchase Date (DDMMYY)",
                    testTagPrefix = "edit_po_date"
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = boxesText,
                        onValueChange = { boxesText = it },
                        label = { Text("Boxes") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = qtyKgsText,
                        onValueChange = { qtyKgsText = it },
                        label = { Text("Qty (kgs)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { rateText = it },
                    label = { Text("Rate per Kg (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Updated Total:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "₹${String.format(Locale.US, "%.2f", total)}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Payment Status", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PurchaseStatus.PENDING_PAYMENT to "Pending",
                        PurchaseStatus.PAID_CASH to "Cash",
                        PurchaseStatus.PAID_UPI to "UPI"
                    ).forEach { (st, label) ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
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
                            val sup = suppliers.firstOrNull { it.name.equals(supplierName, ignoreCase = true) }
                            val updated = purchase.copy(
                                itemName = itemName.trim(),
                                boxes = boxes,
                                qtyKgs = qty,
                                rate = rate,
                                totalAmount = total,
                                supplierName = supplierName.trim(),
                                supplierId = sup?.id ?: purchase.supplierId,
                                date = dateText.trim(),
                                status = selectedStatus
                            )
                            onSave(updated)
                        },
                        enabled = itemName.isNotBlank() && qty > 0.0 && rate > 0.0,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}
