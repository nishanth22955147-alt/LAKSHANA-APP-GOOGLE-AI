package com.example.ui.excel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.util.ExcelSheetParser
import com.example.data.util.SpreadsheetReportExporter
import com.example.ui.MainViewModel
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SpreadsheetTab(val title: String) {
    EXPORT("Export Audit Report"),
    IMPORT("Import Daily Sheet")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySpreadsheetAuditHubDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    initialTab: SpreadsheetTab = SpreadsheetTab.EXPORT,
    initialDate: String = "2026-09-25"
) {
    if (!isOpen) return

    val context = LocalContext.current
    val purchases by viewModel.purchases.collectAsState()
    val excelResult by viewModel.excelParseResult.collectAsState()
    val isProcessing by viewModel.isExcelProcessing.collectAsState()

    var selectedTab by remember { mutableStateOf(initialTab) }

    // --- Export State ---
    var exportDateFilter by remember { mutableStateOf(initialDate) }
    val filteredExportPurchases = remember(purchases, exportDateFilter) {
        if (exportDateFilter.isBlank() || exportDateFilter == "All") {
            purchases
        } else {
            purchases.filter { it.date == exportDateFilter }
        }
    }

    val exportCsvContent = remember(filteredExportPurchases, exportDateFilter) {
        SpreadsheetReportExporter.generateDailySpreadsheetCsv(filteredExportPurchases, exportDateFilter)
    }

    // Document creation launcher for saving/downloading CSV file to device storage
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val success = SpreadsheetReportExporter.writeCsvToUri(context, uri, exportCsvContent)
            if (success) {
                Toast.makeText(context, "Spreadsheet report downloaded & saved successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to write file to device storage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Import State ---
    var importTargetDate by remember { mutableStateOf(initialDate) }
    var rawInputText by remember { mutableStateOf("") }
    var autoUpdateStock by remember { mutableStateOf(true) }
    var defaultSupplier by remember { mutableStateOf("Lakshana Agro Farms") }
    var importedFileName by remember { mutableStateOf<String?>(null) }
    var batchSuccessBatchId by remember { mutableStateOf<String?>(null) }
    var batchSuccessAmount by remember { mutableStateOf(0.0) }

    // File picker launcher for importing CSV/TSV/spreadsheet file from device storage
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    rawInputText = content
                    importedFileName = uri.lastPathSegment ?: "Spreadsheet_File.csv"
                    viewModel.parseDailyExcelText(content, defaultSupplier)
                    Toast.makeText(context, "File loaded: ${importedFileName}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isProcessing) {
                viewModel.clearExcelParseResult()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .testTag("spreadsheet_audit_hub_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SecondaryTeal.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.TableChart,
                                    contentDescription = null,
                                    tint = SecondaryTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Daily Spreadsheet Reports & Auditing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Import daily manifests or export downloadable CSV reports for external audit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.clearExcelParseResult()
                            onDismiss()
                        },
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs: Export vs Import
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SpreadsheetTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (tab == SpreadsheetTab.EXPORT) Icons.Default.FileDownload else Icons.Default.FileUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tab.title, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                when (selectedTab) {
                    SpreadsheetTab.EXPORT -> {
                        ExportAuditReportView(
                            purchases = filteredExportPurchases,
                            csvContent = exportCsvContent,
                            selectedDate = exportDateFilter,
                            onDateChange = { exportDateFilter = it },
                            onDownloadCsv = {
                                val fileName = "Lakshana_Veggie_Audit_${if (exportDateFilter.isBlank() || exportDateFilter == "All") "All_Dates" else exportDateFilter}.csv"
                                createDocLauncher.launch(fileName)
                            },
                            onShareCsv = {
                                val fileName = "Lakshana_Veggie_Audit_${if (exportDateFilter.isBlank() || exportDateFilter == "All") "All_Dates" else exportDateFilter}.csv"
                                SpreadsheetReportExporter.shareReport(context, exportCsvContent, fileName)
                            },
                            onCopyCsv = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Lakshana Veggie Audit CSV", exportCsvContent))
                                Toast.makeText(context, "Spreadsheet report CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    SpreadsheetTab.IMPORT -> {
                        ImportDailySpreadsheetView(
                            rawInputText = rawInputText,
                            onRawInputTextChange = {
                                rawInputText = it
                                viewModel.parseDailyExcelText(it, defaultSupplier)
                            },
                            defaultSupplier = defaultSupplier,
                            onSupplierChange = {
                                defaultSupplier = it
                                if (rawInputText.isNotBlank()) {
                                    viewModel.parseDailyExcelText(rawInputText, it)
                                }
                            },
                            targetDate = importTargetDate,
                            onDateChange = { importTargetDate = it },
                            autoUpdateStock = autoUpdateStock,
                            onAutoUpdateStockChange = { autoUpdateStock = it },
                            importedFileName = importedFileName,
                            onPickFile = { openDocLauncher.launch("*/*") },
                            onLoadSample = {
                                rawInputText = ExcelSheetParser.SAMPLE_DAILY_EXCEL_CSV
                                viewModel.parseDailyExcelText(rawInputText, defaultSupplier)
                            },
                            excelResult = excelResult,
                            isProcessing = isProcessing,
                            batchSuccessBatchId = batchSuccessBatchId,
                            batchSuccessAmount = batchSuccessAmount,
                            onCommitImport = {
                                viewModel.commitDailyExcelImport(autoUpdateStock) { batchId, amount ->
                                    batchSuccessBatchId = batchId
                                    batchSuccessAmount = amount
                                }
                            },
                            onReset = {
                                rawInputText = ""
                                importedFileName = null
                                batchSuccessBatchId = null
                                viewModel.clearExcelParseResult()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportAuditReportView(
    purchases: List<com.example.data.model.PurchaseEntryEntity>,
    csvContent: String,
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onDownloadCsv: () -> Unit,
    onShareCsv: () -> Unit,
    onCopyCsv: () -> Unit
) {
    val totalBoxes = purchases.sumOf { it.boxes }
    val totalKgs = purchases.sumOf { it.qtyKgs }
    val totalAmount = purchases.sumOf { it.totalAmount }
    val totalPaid = purchases.filter {
        it.status == com.example.data.model.PurchaseStatus.PAID ||
                it.status == com.example.data.model.PurchaseStatus.PAID_CASH ||
                it.status == com.example.data.model.PurchaseStatus.PAID_UPI
    }.sumOf { it.totalAmount }
    val totalPending = purchases.filter { it.status == com.example.data.model.PurchaseStatus.PENDING_PAYMENT }.sumOf { it.totalAmount }

    Column(modifier = Modifier.fillMaxSize()) {
        // Date Selector & Filter Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Audit Report Date:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedDate == "All",
                            onClick = { onDateChange("All") },
                            label = { Text("All Dates", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = selectedDate == "2026-09-25",
                            onClick = { onDateChange("2026-09-25") },
                            label = { Text("Today", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = selectedDate == "2026-09-24",
                            onClick = { onDateChange("2026-09-24") },
                            label = { Text("Yesterday", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = onDateChange,
                    label = { Text("Filter Date (YYYY-MM-DD or 'All')") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Audit Metrics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AuditMetricCard("Total Records", "${purchases.size} POs", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            AuditMetricCard("Boxes Count", "$totalBoxes boxes", SecondaryTeal, Modifier.weight(1f))
            AuditMetricCard("Total Weight", "${String.format(Locale.US, "%,.0f", totalKgs)} kgs", Color(0xFF6B4EEA), Modifier.weight(1f))
            AuditMetricCard("Audit Value", "₹${String.format(Locale.US, "%,.0f", totalAmount)}", SuccessGreen, Modifier.weight(1.2f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row (Download CSV, Share with Auditor, Copy)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDownloadCsv,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                modifier = Modifier.weight(1.3f).testTag("download_audit_csv_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Download CSV File")
            }

            OutlinedButton(
                onClick = onShareCsv,
                modifier = Modifier.weight(1f).testTag("share_audit_report_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Report")
            }

            FilledTonalButton(
                onClick = onCopyCsv,
                modifier = Modifier.weight(0.9f).testTag("copy_csv_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy CSV")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "Spreadsheet Preview (${purchases.size} rows ready for export):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Table Preview
        if (purchases.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No purchase records found for date: '$selectedDate'. Select 'All Dates' or record purchases to export.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("Date", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Text("Item Name", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.8f))
                        Text("Boxes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(0.8f))
                        Text("Qty (kg)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Text("Rate (₹)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        Text("Total (₹)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f))
                        Text("Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(purchases, key = { it.id }) { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.date, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(p.itemName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.8f))
                                Text("${p.boxes}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.8f))
                                Text(String.format(Locale.US, "%.1f", p.qtyKgs), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text("₹${String.format(Locale.US, "%.2f", p.rate)}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text("₹${String.format(Locale.US, "%,.2f", p.totalAmount)}", fontWeight = FontWeight.Bold, color = SuccessGreen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.2f))
                                Text(p.status.label, style = MaterialTheme.typography.labelSmall, color = if (p.status == com.example.data.model.PurchaseStatus.PENDING_PAYMENT) WarningAmber else SuccessGreen, modifier = Modifier.weight(1f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditMetricCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ImportDailySpreadsheetView(
    rawInputText: String,
    onRawInputTextChange: (String) -> Unit,
    defaultSupplier: String,
    onSupplierChange: (String) -> Unit,
    targetDate: String,
    onDateChange: (String) -> Unit,
    autoUpdateStock: Boolean,
    onAutoUpdateStockChange: (Boolean) -> Unit,
    importedFileName: String?,
    onPickFile: () -> Unit,
    onLoadSample: () -> Unit,
    excelResult: com.example.data.util.ExcelParseResult?,
    isProcessing: Boolean,
    batchSuccessBatchId: String?,
    batchSuccessAmount: Double,
    onCommitImport: () -> Unit,
    onReset: () -> Unit
) {
    if (batchSuccessBatchId != null) {
        // Success Screen
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = SuccessGreen.copy(alpha = 0.15f), modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("Spreadsheet Ingested Successfully!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Batch ID: $batchSuccessBatchId", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text("Total Value: ₹${String.format(Locale.US, "%,.2f", batchSuccessAmount)}", fontWeight = FontWeight.Bold, color = SuccessGreen)
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onReset) {
                Text("Import Another Sheet")
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Import Source Actions: Pick File vs Load Sample
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onPickFile,
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                modifier = Modifier.testTag("choose_spreadsheet_file_button")
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Browse File (CSV/Excel)")
            }

            OutlinedButton(
                onClick = onLoadSample,
                modifier = Modifier.testTag("load_sample_sheet_button")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Load Sample Rows")
            }
        }

        importedFileName?.let { fname ->
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SecondaryTeal.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Loaded: $fname", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = SecondaryTeal)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = targetDate,
                onValueChange = onDateChange,
                label = { Text("Manifest Date") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = defaultSupplier,
                onValueChange = onSupplierChange,
                label = { Text("Vendor / Farm Supplier") },
                singleLine = true,
                modifier = Modifier.weight(1.5f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = rawInputText,
            onValueChange = onRawInputTextChange,
            label = { Text("Spreadsheet Entries (Date, Items, Boxes, Qty(kgs), Rate, Total)") },
            placeholder = { Text("Date,Items,Boxes,Qty(kgs),Rate,Total\n2026-09-25,Fresh Country Tomatoes,25,500.0,35.00,17500.00\n2026-09-25,Nashik Red Onions,40,1000.0,28.00,28000.00") },
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("import_raw_text_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = autoUpdateStock,
                    onCheckedChange = onAutoUpdateStockChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Auto-update warehouse stock", style = MaterialTheme.typography.bodySmall)
            }

            if (excelResult != null && excelResult.success && excelResult.rows.isNotEmpty()) {
                Button(
                    onClick = onCommitImport,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.testTag("commit_import_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Commit ${excelResult.rows.size} Rows (₹${String.format(Locale.US, "%,.0f", excelResult.grandTotalAmount)})")
                    }
                }
            }
        }
    }
}
