package com.example.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StripeViolet
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = transactions.filter {
        it.stripePaymentIntentId.contains(searchQuery, ignoreCase = true) ||
                it.supplierName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Payment Audit Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = SuccessGreen,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Payment & Settlement Audit Log",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "All vendor settlements recorded via Paid Cash or UPI with instant receipt generation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Voucher / UTR / Reference, Supplier...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Transaction Log (${filtered.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Total Settled: ₹${String.format(Locale.US, "%,.2f", filtered.sumOf { it.amount })}",
                style = MaterialTheme.typography.labelSmall,
                color = SuccessGreen,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions match your search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        onClick = { selectedTransaction = tx }
                    )
                }
            }
        }
    }

    selectedTransaction?.let { tx ->
        StripeReceiptDialog(
            transaction = tx,
            onDismiss = { selectedTransaction = null }
        )
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    onClick: () -> Unit
) {
    val cal = remember(transaction.timestamp) {
        java.util.Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
    }
    val ddmmyyStr = com.example.ui.components.DateFormatterUtil.formatDDMMYY(cal)
    val compactDDMMYY = com.example.ui.components.DateFormatterUtil.formatCompactDDMMYY(cal)
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))

    val isCash = transaction.paymentMethod.contains("Cash", ignoreCase = true)
    val isUpi = transaction.paymentMethod.contains("UPI", ignoreCase = true)
    val badgeColor = when {
        isCash -> SuccessGreen
        isUpi -> SecondaryTeal
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when {
                            isCash -> Icons.Default.Payments
                            isUpi -> Icons.Default.QrCodeScanner
                            else -> Icons.Default.ReceiptLong
                        },
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.supplierName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    transaction.stripePaymentIntentId,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$ddmmyyStr $timeStr",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "DDMMYY: $compactDDMMYY",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    transaction.paymentMethod,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${String.format(Locale.US, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = SuccessGreen
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        transaction.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StripeReceiptDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit
) {
    val dateStr = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    val isCash = transaction.paymentMethod.contains("Cash", ignoreCase = true)
    val isUpi = transaction.paymentMethod.contains("UPI", ignoreCase = true)
    val badgeColor = when {
        isCash -> SuccessGreen
        isUpi -> SecondaryTeal
        else -> MaterialTheme.colorScheme.primary
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
                    .padding(24.dp)
            ) {
                // Receipt Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = badgeColor, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    when {
                                        isCash -> Icons.Default.Payments
                                        isUpi -> Icons.Default.QrCodeScanner
                                        else -> Icons.Default.ReceiptLong
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when {
                                isCash -> "Cash Payment Voucher"
                                isUpi -> "UPI Settlement Receipt"
                                else -> "Settlement Receipt"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                Text("Amount Settled", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "₹${String.format(Locale.US, "%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = SuccessGreen
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Receipt details table
                val txCal = java.util.Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
                val ddmmyy = com.example.ui.components.DateFormatterUtil.formatDDMMYY(txCal)
                val compactDDMMYY = com.example.ui.components.DateFormatterUtil.formatCompactDDMMYY(txCal)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReceiptRow("Payment Intent / Ref", transaction.stripePaymentIntentId)
                    ReceiptRow("Supplier", transaction.supplierName)
                    ReceiptRow("Settlement Date (DDMMYY)", "$ddmmyy (DDMMYY: $compactDDMMYY)")
                    ReceiptRow("Recorded Timestamp", dateStr)
                    ReceiptRow("Payment Method", transaction.paymentMethod)
                    ReceiptRow("Authorized By", transaction.processedBy)
                    ReceiptRow("Status", transaction.status.label)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Receipt")
                }
            }
        }
    }
}

@Composable
fun ReceiptRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
