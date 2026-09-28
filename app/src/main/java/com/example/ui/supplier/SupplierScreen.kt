package com.example.ui.supplier

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PurchaseEntryEntity
import com.example.data.model.PurchaseStatus
import com.example.data.model.SupplierEntity
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun SupplierScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role == UserRole.ADMIN
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var viewingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var deletingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else {
            suppliers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.contactPerson.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery, ignoreCase = true) ||
                        it.vegetableCategories.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Total outstanding payables calculated from daily ledger
    val totalLedgerOutstanding = remember(purchases, suppliers) {
        val unpaidFromPurchases = purchases.filter {
            it.status != PurchaseStatus.PAID && it.status != PurchaseStatus.PAID_CASH && it.status != PurchaseStatus.PAID_UPI
        }.sumOf { it.totalAmount }
        if (unpaidFromPurchases > 0.0) unpaidFromPurchases else suppliers.sumOf { it.outstandingPayable }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isAddDialogOpen = true },
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Supplier") },
                modifier = Modifier.testTag("add_supplier_fab")
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

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("supplier_search_input"),
                placeholder = { Text("Search by name, vegetable, phone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Header summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Vegetable Suppliers (${filteredSuppliers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Ledger Outstanding: ₹${String.format(Locale.US, "%,.2f", totalLedgerOutstanding)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Storefront,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (searchQuery.isBlank()) "No vegetable suppliers registered yet."
                            else "No suppliers matching '$searchQuery'",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { isAddDialogOpen = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Register First Supplier")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSuppliers, key = { it.id }) { supplier ->
                        val vendorPurchases = remember(purchases, supplier) {
                            purchases.filter {
                                (supplier.id > 0 && it.supplierId == supplier.id) ||
                                        it.supplierName.trim().equals(supplier.name.trim(), ignoreCase = true)
                            }
                        }
                        val unpaidPurchases = remember(vendorPurchases) {
                            vendorPurchases.filter {
                                it.status != PurchaseStatus.PAID && it.status != PurchaseStatus.PAID_CASH && it.status != PurchaseStatus.PAID_UPI
                            }
                        }
                        val outstandingBalance = remember(unpaidPurchases, supplier.outstandingPayable) {
                            val ledgerUnpaid = unpaidPurchases.sumOf { it.totalAmount }
                            if (ledgerUnpaid > 0.0) ledgerUnpaid else supplier.outstandingPayable
                        }
                        val totalSourced = remember(vendorPurchases, supplier.totalPurchasesAmount) {
                            val ledgerTotal = vendorPurchases.sumOf { it.totalAmount }
                            if (ledgerTotal > 0.0) ledgerTotal else supplier.totalPurchasesAmount
                        }
                        val totalSettled = remember(vendorPurchases, supplier.totalPaidAmount) {
                            val ledgerSettled = vendorPurchases.filter {
                                it.status == PurchaseStatus.PAID || it.status == PurchaseStatus.PAID_CASH || it.status == PurchaseStatus.PAID_UPI
                            }.sumOf { it.totalAmount }
                            if (ledgerSettled > 0.0) ledgerSettled else supplier.totalPaidAmount
                        }

                        SupplierContactCard(
                            supplier = supplier,
                            isAdmin = isAdmin,
                            outstandingBalance = outstandingBalance,
                            unpaidCount = unpaidPurchases.size,
                            totalSourced = totalSourced,
                            totalSettled = totalSettled,
                            onCardClick = { viewingSupplier = supplier },
                            onEdit = { editingSupplier = supplier },
                            onDelete = { deletingSupplier = supplier },
                            onCall = { dialPhoneNumber(context, supplier.phone) },
                            onEmail = { sendEmail(context, supplier.email, supplier.name) },
                            onWhatsApp = { openWhatsApp(context, supplier.phone) }
                        )
                    }
                }
            }
        }
    }

    // View Details Modal
    viewingSupplier?.let { supplier ->
        val vPurchases = remember(purchases, supplier) {
            purchases.filter {
                (supplier.id > 0 && it.supplierId == supplier.id) ||
                        it.supplierName.trim().equals(supplier.name.trim(), ignoreCase = true)
            }
        }
        val unPurchases = remember(vPurchases) {
            vPurchases.filter {
                it.status != PurchaseStatus.PAID && it.status != PurchaseStatus.PAID_CASH && it.status != PurchaseStatus.PAID_UPI
            }
        }
        val outBal = remember(unPurchases, supplier.outstandingPayable) {
            val lu = unPurchases.sumOf { it.totalAmount }
            if (lu > 0.0) lu else supplier.outstandingPayable
        }
        val totSrc = remember(vPurchases, supplier.totalPurchasesAmount) {
            val lt = vPurchases.sumOf { it.totalAmount }
            if (lt > 0.0) lt else supplier.totalPurchasesAmount
        }
        val totSet = remember(vPurchases, supplier.totalPaidAmount) {
            val ls = vPurchases.filter {
                it.status == PurchaseStatus.PAID || it.status == PurchaseStatus.PAID_CASH || it.status == PurchaseStatus.PAID_UPI
            }.sumOf { it.totalAmount }
            if (ls > 0.0) ls else supplier.totalPaidAmount
        }

        SupplierDetailDialog(
            supplier = supplier,
            isAdmin = isAdmin,
            outstandingBalance = outBal,
            unpaidCount = unPurchases.size,
            totalSourced = totSrc,
            totalSettled = totSet,
            onDismiss = { viewingSupplier = null },
            onEdit = {
                val s = viewingSupplier
                viewingSupplier = null
                editingSupplier = s
            },
            onDelete = {
                val s = viewingSupplier
                viewingSupplier = null
                deletingSupplier = s
            },
            onCall = { dialPhoneNumber(context, supplier.phone) },
            onEmail = { sendEmail(context, supplier.email, supplier.name) },
            onWhatsApp = { openWhatsApp(context, supplier.phone) }
        )
    }

    // Add or Edit Supplier Dialog
    if (isAddDialogOpen || editingSupplier != null) {
        SupplierFormDialog(
            supplier = editingSupplier,
            onDismiss = {
                isAddDialogOpen = false
                editingSupplier = null
            },
            onSave = { saved ->
                viewModel.saveSupplier(saved)
                isAddDialogOpen = false
                editingSupplier = null
            }
        )
    }

    // Delete Confirmation Dialog (Admin only)
    deletingSupplier?.let { supplier ->
        AlertDialog(
            onDismissRequest = { deletingSupplier = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Supplier?") },
            text = { Text("Are you sure you want to permanently delete '${supplier.name}'? Only administrators are authorized to remove suppliers.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSupplier(supplier)
                        deletingSupplier = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Supplier")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSupplier = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SupplierContactCard(
    supplier: SupplierEntity,
    isAdmin: Boolean,
    outstandingBalance: Double = supplier.outstandingPayable,
    unpaidCount: Int = 0,
    totalSourced: Double = supplier.totalPurchasesAmount,
    totalSettled: Double = supplier.totalPaidAmount,
    onCardClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("supplier_card_${supplier.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                supplier.name.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            supplier.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Contact: ${supplier.contactPerson}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (isAdmin) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vegetable Categories Badge
            if (supplier.vegetableCategories.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Grass,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            supplier.vegetableCategories,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Quick Contact Actions Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quick Call button
                FilledTonalButton(
                    onClick = onCall,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call", style = MaterialTheme.typography.labelSmall)
                }

                // Quick WhatsApp button
                FilledTonalButton(
                    onClick = onWhatsApp,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = SuccessGreen.copy(alpha = 0.15f),
                        contentColor = SuccessGreen
                    )
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                }

                // Quick Email button
                if (supplier.email.isNotBlank()) {
                    FilledTonalButton(
                        onClick = onEmail,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = "Email", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Email", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // NEW: Dedicated Outstanding Balance Box (Pulled from Daily Ledger)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (outstandingBalance > 0) WarningAmber.copy(alpha = 0.12f) else SuccessGreen.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, if (outstandingBalance > 0) WarningAmber.copy(alpha = 0.35f) else SuccessGreen.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = if (outstandingBalance > 0) WarningAmber else SuccessGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "OUTSTANDING BALANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (outstandingBalance > 0) WarningAmber else SuccessGreen,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            if (unpaidCount > 0) "Pulled from daily ledger: $unpaidCount unpaid bills"
                            else "✓ All daily ledger purchases settled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        "₹${String.format(Locale.US, "%,.2f", outstandingBalance)}",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (outstandingBalance > 0) WarningAmber else SuccessGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // 3-Column Financial Balance Stats with Outstanding Balance Column
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Purchases", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.US, "%,.2f", totalSourced)}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Settled (Paid)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${String.format(Locale.US, "%,.2f", totalSettled)}", fontWeight = FontWeight.SemiBold, color = SuccessGreen, style = MaterialTheme.typography.bodySmall)
                }
                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                    Text("Outstanding Balance", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (outstandingBalance > 0) WarningAmber else SuccessGreen)
                    Text(
                        "₹${String.format(Locale.US, "%,.2f", outstandingBalance)}",
                        fontWeight = FontWeight.Bold,
                        color = if (outstandingBalance > 0) WarningAmber else SuccessGreen,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierDetailDialog(
    supplier: SupplierEntity,
    isAdmin: Boolean,
    outstandingBalance: Double = supplier.outstandingPayable,
    unpaidCount: Int = 0,
    totalSourced: Double = supplier.totalPurchasesAmount,
    totalSettled: Double = supplier.totalPaidAmount,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onEmail: () -> Unit,
    onWhatsApp: () -> Unit
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
                // Header with name & close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(supplier.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Vegetable Supplier Profile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCall,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call")
                    }
                    Button(
                        onClick = onWhatsApp,
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp")
                    }
                    if (supplier.email.isNotBlank()) {
                        Button(
                            onClick = onEmail,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Rows
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRowItem(Icons.Default.Person, "Contact Person", supplier.contactPerson)
                    DetailRowItem(Icons.Default.Phone, "Mobile Number", supplier.phone)
                    if (supplier.email.isNotBlank()) {
                        DetailRowItem(Icons.Default.Email, "Email Address", supplier.email)
                    }
                    if (supplier.vegetableCategories.isNotBlank()) {
                        DetailRowItem(Icons.Default.Eco, "Vegetables Supplied", supplier.vegetableCategories)
                    }
                    if (supplier.paymentUpiId.isNotBlank()) {
                        DetailRowItem(Icons.Default.QrCode, "Payment UPI / VPA", supplier.paymentUpiId)
                    }
                    if (supplier.address.isNotBlank()) {
                        DetailRowItem(Icons.Default.LocationOn, "Mandi / Yard Address", supplier.address)
                    }
                    if (supplier.notes.isNotBlank()) {
                        DetailRowItem(Icons.Default.Notes, "Notes & Terms", supplier.notes)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Financial Overview
                Text("Procurement Financials", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Sourced Amount", style = MaterialTheme.typography.bodySmall)
                            Text("₹${String.format(Locale.US, "%,.2f", totalSourced)}", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Paid (Cash / UPI)", style = MaterialTheme.typography.bodySmall)
                            Text("₹${String.format(Locale.US, "%,.2f", totalSettled)}", fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Outstanding Balance", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (outstandingBalance > 0) WarningAmber else SuccessGreen)
                                Text("Pulled from daily ledger: $unpaidCount unpaid bills", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "₹${String.format(Locale.US, "%,.2f", outstandingBalance)}",
                                fontWeight = FontWeight.Black,
                                color = if (outstandingBalance > 0) WarningAmber else SuccessGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Details")
                    }
                    if (isAdmin) {
                        Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRowItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SupplierFormDialog(
    supplier: SupplierEntity?,
    onDismiss: () -> Unit,
    onSave: (SupplierEntity) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var contact by remember { mutableStateOf(supplier?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var vegetableCategories by remember { mutableStateOf(supplier?.vegetableCategories ?: "Tomatoes, Onions, Potatoes, Greens") }
    var paymentUpiId by remember { mutableStateOf(supplier?.paymentUpiId ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }

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
                Text(
                    if (supplier == null) "Register Vegetable Supplier" else "Edit Supplier Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Farm / Business / Supplier Name *") },
                    placeholder = { Text("e.g. Ramesh Agro Farms") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("Contact Person Name *") },
                    placeholder = { Text("e.g. Ramesh Kumar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_contact_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone / Mobile Number *") },
                    placeholder = { Text("e.g. +91 98450 12345") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_phone_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    placeholder = { Text("e.g. ramesh@veggiesupply.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = vegetableCategories,
                    onValueChange = { vegetableCategories = it },
                    label = { Text("Vegetable Varieties Supplied") },
                    placeholder = { Text("e.g. Tomatoes, Onions, Carrots, Capsicum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = paymentUpiId,
                    onValueChange = { paymentUpiId = it },
                    label = { Text("Supplier UPI ID / VPA for Payments") },
                    placeholder = { Text("e.g. rameshkumar@okaxis") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Mandi / Yard / Farm Address") },
                    placeholder = { Text("e.g. Shop #12, Wholesale APMC Market") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Delivery Terms & Notes") },
                    placeholder = { Text("e.g. Daily morning delivery at 6 AM, Net 7") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val updated = (supplier ?: SupplierEntity(
                                name = name.trim(),
                                contactPerson = contact.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                vegetableCategories = vegetableCategories.trim(),
                                paymentUpiId = paymentUpiId.trim(),
                                notes = notes.trim()
                            )).copy(
                                name = name.trim(),
                                contactPerson = contact.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                vegetableCategories = vegetableCategories.trim(),
                                paymentUpiId = paymentUpiId.trim(),
                                notes = notes.trim()
                            )
                            onSave(updated)
                        },
                        enabled = name.isNotBlank() && phone.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("save_supplier_button")
                    ) {
                        Text("Save Supplier")
                    }
                }
            }
        }
    }
}

// Helpers for Phone, WhatsApp and Email
fun dialPhoneNumber(context: Context, phoneNumber: String) {
    try {
        val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanPhone")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to dial: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

fun openWhatsApp(context: Context, phoneNumber: String) {
    try {
        val cleanPhone = phoneNumber.filter { it.isDigit() }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/$cleanPhone")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not available, opening dialer...", Toast.LENGTH_SHORT).show()
        dialPhoneNumber(context, phoneNumber)
    }
}

fun sendEmail(context: Context, emailAddress: String, supplierName: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$emailAddress")
            putExtra(Intent.EXTRA_SUBJECT, "Lakshana Veggie Procurement Inquiry - $supplierName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open email client", Toast.LENGTH_SHORT).show()
    }
}
