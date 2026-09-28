package com.example.ui.reports

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PurchaseStatus
import com.example.ui.MainViewModel
import com.example.ui.excel.DailySpreadsheetAuditHubDialog
import com.example.ui.excel.SpreadsheetTab
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.util.Locale

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val purchases by viewModel.purchases.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val items by viewModel.items.collectAsState()
    val context = LocalContext.current

    var isAuditHubOpen by remember { mutableStateOf(false) }
    var auditHubTab by remember { mutableStateOf(SpreadsheetTab.EXPORT) }

    val totalPurchased = purchases.sumOf { it.totalAmount }
    val totalPaidStripe = transactions.filter { it.status.name == "SUCCEEDED" }.sumOf { it.amount }
    val totalOutstanding = purchases.filter { it.status == PurchaseStatus.PENDING_PAYMENT }.sumOf { it.totalAmount }
    val totalStockValuation = items.sumOf { it.currentStockKgs * it.defaultRatePerKg }

    // Supplier expenditure grouping
    val supplierTotals = purchases.groupBy { it.supplierName }
        .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
        .toList()
        .sortedByDescending { it.second }

    // Date grouping
    val dateTotals = purchases.groupBy { it.date }
        .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
        .toList()
        .sortedByDescending { it.first }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Procurement & Financial Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Comprehensive analysis of purchases, Cash & UPI settlements & stock valuation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        val reportText = buildString {
                            appendLine("=== LAKSHANA VEGGIE REPORT ===")
                            appendLine("Total Purchases: ₹${String.format(Locale.US, "%.2f", totalPurchased)}")
                            appendLine("Paid (Cash / UPI): ₹${String.format(Locale.US, "%.2f", totalPaidStripe)}")
                            appendLine("Pending Payables: ₹${String.format(Locale.US, "%.2f", totalOutstanding)}")
                            appendLine("Inventory Stock Valuation: ₹${String.format(Locale.US, "%.2f", totalStockValuation)}")
                            appendLine("\n--- TOP SUPPLIERS ---")
                            supplierTotals.forEach { (sup, amt) ->
                                appendLine("$sup: ₹${String.format(Locale.US, "%.2f", amt)}")
                            }
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Lakshana Veggie Report", reportText))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Report")
                }
            }
        }

        // Daily Spreadsheet Reports: Downloadable CSV Export & File Import Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SecondaryTeal.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Daily Spreadsheet Reports & Auditing",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryTeal
                            )
                            Text(
                                "Import daily manifests or export downloadable CSV spreadsheets for external auditing",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                auditHubTab = SpreadsheetTab.EXPORT
                                isAuditHubOpen = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV Report")
                        }

                        OutlinedButton(
                            onClick = {
                                auditHubTab = SpreadsheetTab.IMPORT
                                isAuditHubOpen = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Sheet")
                        }
                    }
                }
            }
        }

        // High Level Metrics
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Financial Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Total Sourced", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format(Locale.US, "%,.2f", totalPurchased)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Column {
                            Text("Cash/UPI Settled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format(Locale.US, "%,.2f", totalPaidStripe)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = SuccessGreen)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Outstanding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format(Locale.US, "%,.2f", totalOutstanding)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = WarningAmber)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current Warehouse Stock Valuation:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "₹${String.format(Locale.US, "%,.2f", totalStockValuation)}",
                            fontWeight = FontWeight.Black,
                            color = SecondaryTeal
                        )
                    }
                }
            }
        }

        // Supplier Breakdown
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Supplier-wise Sourcing Breakdown",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (supplierTotals.isEmpty()) {
                        Text("No supplier data available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val maxAmt = supplierTotals.maxOfOrNull { it.second } ?: 1.0
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            supplierTotals.forEach { (sup, amt) ->
                                val progress = (amt / maxAmt).toFloat().coerceIn(0.05f, 1f)
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(sup, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                        Text("₹${String.format(Locale.US, "%,.2f", amt)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = PrimaryBlue,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Date-wise Purchase Activity
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Daily Purchase History",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (dateTotals.isEmpty()) {
                        Text("No date records available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            dateTotals.forEach { (date, amt) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(date, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        "₹${String.format(Locale.US, "%,.2f", amt)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Daily Spreadsheet Audit Export & File Import Dialog
    DailySpreadsheetAuditHubDialog(
        isOpen = isAuditHubOpen,
        onDismiss = { isAuditHubOpen = false },
        viewModel = viewModel,
        initialTab = auditHubTab
    )
}
