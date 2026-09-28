package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ItemEntity
import com.example.data.model.PurchaseEntryEntity
import com.example.data.model.PurchaseStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.UserRole
import com.example.ui.MainViewModel
import com.example.ui.NavigationDestination
import com.example.ui.components.LakshanaLogoHeader
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onOpenExcelUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val purchases by viewModel.purchases.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val items by viewModel.items.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val totalPurchasedAmount = purchases.sumOf { it.totalAmount }
    val totalPaidViaStripe = transactions.filter { it.status.name == "SUCCEEDED" }.sumOf { it.amount }
    val pendingAmount = purchases.filter { it.status == PurchaseStatus.PENDING_PAYMENT }.sumOf { it.totalAmount }
    val totalStockKgs = items.sumOf { it.currentStockKgs }
    val lowStockItems = items.filter { it.isLowStock }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Logo Banner matching attached image
        item {
            LakshanaLogoHeader(
                height = 110.dp,
                showContainerCard = true
            )
        }

        // Welcome Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Welcome back, ${currentUser?.fullName ?: "Staff"}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Role: ${currentUser?.role?.displayName ?: "Purchaser"} • Department: ${currentUser?.department ?: "Procurement"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                "Quick Workflows",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    QuickActionCard(
                        title = "Daily Excel Upload",
                        subtitle = "Bulk stock import",
                        icon = Icons.Default.TableChart,
                        color = SecondaryTeal,
                        onClick = onOpenExcelUpload
                    )
                }
                item {
                    QuickActionCard(
                        title = "New Purchase",
                        subtitle = "Record order",
                        icon = Icons.Default.AddShoppingCart,
                        color = PrimaryBlue,
                        onClick = { viewModel.navigateTo(NavigationDestination.Purchase) }
                    )
                }
                item {
                    QuickActionCard(
                        title = "Cash & UPI Pay",
                        subtitle = "Settle purchases",
                        icon = Icons.Default.Payments,
                        color = SuccessGreen,
                        onClick = { viewModel.navigateTo(NavigationDestination.Transaction) }
                    )
                }
                item {
                    QuickActionCard(
                        title = "Inventory Audit",
                        subtitle = "Stock adjustments",
                        icon = Icons.Default.Inventory2,
                        color = WarningAmber,
                        onClick = { viewModel.navigateTo(NavigationDestination.Inventory) }
                    )
                }
            }
        }

        // Financial & Inventory KPI Summary Cards
        item {
            Text(
                "Executive Summary & Metrics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Total Purchases",
                        value = "₹${String.format(Locale.US, "%,.2f", totalPurchasedAmount)}",
                        caption = "${purchases.size} purchase entries",
                        icon = Icons.Default.ShoppingBag,
                        color = PrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Paid (Cash / UPI)",
                        value = "₹${String.format(Locale.US, "%,.2f", totalPaidViaStripe)}",
                        caption = "Settled & Verified",
                        icon = Icons.Default.CheckCircle,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Pending Payable",
                        value = "₹${String.format(Locale.US, "%,.2f", pendingAmount)}",
                        caption = "Awaiting settlement",
                        icon = Icons.Default.PendingActions,
                        color = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Warehouse Stock",
                        value = "${String.format(Locale.US, "%,.0f", totalStockKgs)} kgs",
                        caption = "${items.size} catalog items",
                        icon = Icons.Default.Warehouse,
                        color = SecondaryTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Low Stock Alert Banner (if any)
        if (lowStockItems.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRose.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.navigateTo(NavigationDestination.Inventory) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = ErrorRose,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Low Stock Alert (${lowStockItems.size} items)",
                                fontWeight = FontWeight.Bold,
                                color = ErrorRose,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                lowStockItems.joinToString(", ") { "${it.name} (${it.currentStockKgs} kgs)" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            Icons.Default.ArrowForwardIos,
                            contentDescription = null,
                            tint = ErrorRose,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Recent Transactions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Payment Transactions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { viewModel.navigateTo(NavigationDestination.Transaction) }) {
                    Text("View All")
                }
            }

            if (transactions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "No payment transactions recorded yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    transactions.take(4).forEach { tx ->
                        RecentTransactionRow(tx = tx)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.12f),
        modifier = Modifier
            .width(155.dp)
            .height(105.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    caption: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = CircleShape,
                    color = color.copy(alpha = 0.15f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentTransactionRow(tx: TransactionEntity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = StripeViolet.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = StripeViolet, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tx.supplierName,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    tx.stripePaymentIntentId,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${String.format(Locale.US, "%.2f", tx.amount)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SuccessGreen
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        tx.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
