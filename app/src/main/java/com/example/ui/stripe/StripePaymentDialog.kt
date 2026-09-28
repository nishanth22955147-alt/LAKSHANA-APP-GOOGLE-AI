package com.example.ui.stripe

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.MainViewModel
import com.example.ui.PaymentMode
import com.example.ui.PaymentModalState
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StripeViolet
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.components.DateFormatterUtil
import com.example.ui.components.DateForwardBackwardSelector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StripePaymentDialog(
    state: PaymentModalState,
    viewModel: MainViewModel
) {
    if (!state.isOpen) return

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Settlement Date state (DDMMYY with Forward / Backward navigation)
    var settlementDate by remember(state.purchase?.id, state.batchId) {
        mutableStateOf(DateFormatterUtil.todayDDMMYY())
    }

    // Cash state
    var cashTenderedText by remember(state.amount) {
        mutableStateOf(String.format(Locale.US, "%.2f", state.amount))
    }
    var cashVoucherNo by remember(state.purchase?.id, state.batchId) {
        val rand = (1000..9999).random()
        mutableStateOf("CASH-VCH-$rand")
    }
    var cashNotes by remember { mutableStateOf("Immediate cash handover at counter") }

    // UPI state
    var selectedUpiApp by remember { mutableStateOf("Google Pay") }
    var upiIdInput by remember(state.supplierName) {
        val cleanName = state.supplierName.lowercase().replace(" ", "").take(10)
        mutableStateOf("$cleanName@okaxis")
    }
    var utrInput by remember {
        val randomUtr = (100000000000L..999999999999L).random().toString()
        mutableStateOf(randomUtr)
    }

    val cashTendered = cashTenderedText.toDoubleOrNull() ?: state.amount
    val changeDue = (cashTendered - state.amount).coerceAtLeast(0.0)

    val isSettled = state.successRefId != null || state.result?.isSuccess == true

    Dialog(
        onDismissRequest = {
            if (!state.isProcessing) viewModel.closePaymentModal()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("payment_modal_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = when (state.activeTab) {
                                PaymentMode.UPI -> SecondaryTeal.copy(alpha = 0.15f)
                                else -> SuccessGreen.copy(alpha = 0.15f)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    when (state.activeTab) {
                                        PaymentMode.UPI -> Icons.Default.QrCodeScanner
                                        else -> Icons.Default.Payments
                                    },
                                    contentDescription = null,
                                    tint = when (state.activeTab) {
                                        PaymentMode.UPI -> SecondaryTeal
                                        else -> SuccessGreen
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Purchase Payment Settlement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Accepted Modes: Paid Cash or UPI Only",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (!state.isProcessing && !isSettled) {
                        IconButton(onClick = { viewModel.closePaymentModal() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                if (isSettled) {
                    // Success Receipt View
                    val refId = state.successRefId ?: state.result?.paymentIntentId ?: "SETTLED"
                    val mode = state.successMode ?: "Cash / UPI"

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SuccessGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(42.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Payment Received & Settled!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹${String.format(Locale.US, "%.2f", state.amount)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Receipt Details
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Payment Mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(mode, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                                val effectiveSettlementDate = state.settlementDate ?: settlementDate
                                val settledCal = remember(effectiveSettlementDate) { DateFormatterUtil.parseToCalendar(effectiveSettlementDate) }
                                val displaySettled = DateFormatterUtil.formatDDMMYY(settledCal)
                                val compactSettled = DateFormatterUtil.formatCompactDDMMYY(settledCal)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Settlement Date (DDMMYY)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$displaySettled (DDMMYY: $compactSettled)", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Reference / UTR / Voucher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(refId, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Supplier", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(state.supplierName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Purchase Reference", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(state.purchase?.batchId ?: state.batchId ?: "PO", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("COMPLETED & PAID", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.closePaymentModal() },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Done & Return to Orders")
                        }
                    }
                } else {
                    // Payment Setup View (Cash or UPI)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState)
                    ) {
                        // Order Summary Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        if (state.purchase != null) "PO: ${state.purchase.batchId} (${state.purchase.itemName})"
                                        else "Bulk Batch: ${state.batchId}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Vendor: ${state.supplierName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "₹${String.format(Locale.US, "%.2f", state.amount)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Tab Selector: Cash vs UPI Only
                        TabRow(
                            selectedTabIndex = if (state.activeTab == PaymentMode.UPI) 1 else 0
                        ) {
                            Tab(
                                selected = state.activeTab != PaymentMode.UPI,
                                onClick = { viewModel.setPaymentActiveTab(PaymentMode.CASH) },
                                text = { Text("💵 Cash Payment") },
                                modifier = Modifier.testTag("tab_payment_cash")
                            )
                            Tab(
                                selected = state.activeTab == PaymentMode.UPI,
                                onClick = { viewModel.setPaymentActiveTab(PaymentMode.UPI) },
                                text = { Text("📱 UPI Instant") },
                                modifier = Modifier.testTag("tab_payment_upi")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tab Content
                        when (state.activeTab) {
                            PaymentMode.CASH -> {
                                // --- Cash Payment Workflow ---
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Settlement Date Forward & Backward Selector (DDMMYY)
                                    DateForwardBackwardSelector(
                                        selectedDateStr = settlementDate,
                                        onDateChange = { settlementDate = it },
                                        label = "Cash Settlement Date (DDMMYY)",
                                        testTagPrefix = "cash_settlement_date"
                                    )

                                    OutlinedTextField(
                                        value = cashTenderedText,
                                        onValueChange = { cashTenderedText = it },
                                        label = { Text("Cash Amount Handed Over (₹)") },
                                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("cash_tendered_input")
                                    )

                                    if (changeDue > 0.0) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SuccessGreen.copy(alpha = 0.12f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Change Return Due:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                                Text(
                                                    "₹${String.format(Locale.US, "%.2f", changeDue)}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = SuccessGreen
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = cashVoucherNo,
                                        onValueChange = { cashVoucherNo = it },
                                        label = { Text("Cash Voucher / Physical Receipt Ref #") },
                                        leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("cash_voucher_input")
                                    )

                                    OutlinedTextField(
                                        value = cashNotes,
                                        onValueChange = { cashNotes = it },
                                        label = { Text("Notes / Denominations (Optional)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            viewModel.executeCashPayment(cashTendered, cashVoucherNo, cashNotes, settlementDate)
                                        },
                                        enabled = !state.isProcessing && cashTendered >= state.amount,
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_cash_payment_button")
                                    ) {
                                        if (state.isProcessing) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Recording Cash Settlement...")
                                        } else {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Confirm Cash Payment (₹${String.format(Locale.US, "%.2f", state.amount)})")
                                        }
                                    }
                                }
                            }

                            PaymentMode.UPI -> {
                                // --- UPI Payment Workflow ---
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Settlement Date Forward & Backward Selector (DDMMYY)
                                    DateForwardBackwardSelector(
                                        selectedDateStr = settlementDate,
                                        onDateChange = { settlementDate = it },
                                        label = "UPI Settlement Date (DDMMYY)",
                                        testTagPrefix = "upi_settlement_date"
                                    )

                                    Text("Select UPI App / Provider", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Google Pay", "PhonePe", "Paytm", "BHIM").forEach { app ->
                                            FilterChip(
                                                selected = selectedUpiApp == app,
                                                onClick = { selectedUpiApp = app },
                                                label = { Text(app, style = MaterialTheme.typography.labelSmall) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    // UPI QR / VPA Card
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = SecondaryTeal.copy(alpha = 0.08f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.QrCode2,
                                                contentDescription = "UPI QR Code",
                                                tint = SecondaryTeal,
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Scan QR or Pay to Supplier VPA",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryTeal
                                            )
                                            Text(
                                                upiIdInput,
                                                fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", upiIdInput))
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Copy UPI ID", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = upiIdInput,
                                        onValueChange = { upiIdInput = it },
                                        label = { Text("Supplier UPI ID / VPA") },
                                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("upi_id_input")
                                    )

                                    OutlinedTextField(
                                        value = utrInput,
                                        onValueChange = { utrInput = it },
                                        label = { Text("12-Digit UPI Ref / UTR Number") },
                                        placeholder = { Text("e.g. 928471029384") },
                                        leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                utrInput = (100000000000L..999999999999L).random().toString()
                                            }) {
                                                Icon(Icons.Default.Refresh, contentDescription = "Generate")
                                            }
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth().testTag("upi_utr_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            viewModel.executeUpiPayment(upiIdInput.trim(), utrInput.trim(), selectedUpiApp, settlementDate)
                                        },
                                        enabled = !state.isProcessing && upiIdInput.isNotBlank() && utrInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_upi_payment_button")
                                    ) {
                                        if (state.isProcessing) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Verifying UPI Payment...")
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Verify & Settle via UPI (₹${String.format(Locale.US, "%.2f", state.amount)})")
                                        }
                                    }
                                }
                            }

                            PaymentMode.STRIPE -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Payment Accepted via Cash or UPI Only",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Please select Cash Payment or UPI Instant above to record and verify settlement.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { viewModel.setPaymentActiveTab(PaymentMode.CASH) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Text("Pay with Cash")
                                        }
                                        Button(
                                            onClick = { viewModel.setPaymentActiveTab(PaymentMode.UPI) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal)
                                        ) {
                                            Text("Pay with UPI")
                                        }
                                    }
                                }
                            }
                        }

                        // Error display
                        state.errorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    err,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
