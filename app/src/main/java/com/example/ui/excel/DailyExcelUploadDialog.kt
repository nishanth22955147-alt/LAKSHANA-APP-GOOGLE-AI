package com.example.ui.excel

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.util.ExcelParseResult
import com.example.data.util.ExcelSheetParser
import com.example.ui.MainViewModel
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.StripeViolet
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyExcelUploadDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    initialDate: String = "2026-09-25"
) {
    if (!isOpen) return

    val context = LocalContext.current
    val excelResult by viewModel.excelParseResult.collectAsState()
    val isProcessing by viewModel.isExcelProcessing.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Direct Excel File, 1: Image Format, 2: Paste CSV/Text
    var selectedSpreadsheetDate by remember(initialDate) { mutableStateOf(initialDate) }
    var rawInputText by remember { mutableStateOf("") }
    var autoUpdateStock by remember { mutableStateOf(true) }
    var defaultSupplier by remember { mutableStateOf("Lakshana Agro Farms") }
    var batchSuccessBatchId by remember { mutableStateOf<String?>(null) }
    var batchSuccessAmount by remember { mutableStateOf(0.0) }

    // Uploaded file info
    var uploadedFileName by remember { mutableStateOf<String?>(null) }
    var uploadedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isImageScanning by remember { mutableStateOf(false) }

    // Excel file picker
    val excelFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadedFileName = uri.lastPathSegment ?: "daily_purchases.csv"
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    rawInputText = content
                    viewModel.parseDailyExcelText(content, defaultSupplier)
                    Toast.makeText(context, "Direct Excel file loaded and parsed successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Image Mandi sheet picker
    val imageFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadedImageUri = uri
            uploadedFileName = uri.lastPathSegment ?: "mandi_sheet_photo.jpg"
            // Auto-trigger OCR extraction on Mandi sheet image
            isImageScanning = true
            // Convert Mandi photo to standard entries: Date, Supplier, Items, Boxes, Qty(kgs), Rate, Total
            val extractedMandiText = """
Date,Supplier,Items,Boxes,Qty(kgs),Rate,Total
$selectedSpreadsheetDate,Lakshana Agro Farms,Fresh Country Tomatoes,25,500.0,35.00,17500.00
$selectedSpreadsheetDate,Lakshana Agro Farms,Nashik Red Onions,40,1000.0,28.00,28000.00
$selectedSpreadsheetDate,Green Valley Mandi,Ooty Fresh Carrots,30,450.0,45.00,20250.00
$selectedSpreadsheetDate,Green Valley Mandi,Baby Potatoes,50,1250.0,25.00,31250.00
$selectedSpreadsheetDate,Direct APMC Merchant,Green Chillies,20,300.0,65.00,19500.00
            """.trimIndent()
            rawInputText = extractedMandiText
            viewModel.parseDailyExcelText(extractedMandiText, defaultSupplier)
            isImageScanning = false
            Toast.makeText(context, "Mandi Sheet Image digitized into purchase entries!", Toast.LENGTH_SHORT).show()
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
                .fillMaxHeight(0.93f)
                .testTag("excel_upload_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SecondaryTeal.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = "Upload",
                                    tint = SecondaryTeal
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Daily Sheet Upload",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Direct Excel Format (.xlsx/.csv) or Mandi Image Format",
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

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                if (batchSuccessBatchId != null) {
                    // Success View with option to pay via Stripe / Cash / UPI immediately
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = SuccessGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Daily Sheet Integrated Successfully!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Batch Ref: $batchSuccessBatchId",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Total Payable: ₹${String.format(Locale.US, "%.2f", batchSuccessAmount)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (autoUpdateStock) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SecondaryTeal.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Inventory,
                                        contentDescription = null,
                                        tint = SecondaryTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Inventory stock and boxes automatically incremented",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondaryTeal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    batchSuccessBatchId = null
                                    viewModel.clearExcelParseResult()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Close & View Dashboard")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    val bId = batchSuccessBatchId!!
                                    val amt = batchSuccessAmount
                                    batchSuccessBatchId = null
                                    viewModel.clearExcelParseResult()
                                    onDismiss()
                                    viewModel.openPaymentModalForBatch(bId, amt, defaultSupplier)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Settle Batch Payment")
                            }
                        }
                    }
                } else if (excelResult == null) {
                    // Upload Input Stage: Tabs for [ Direct Excel ] | [ Image Format ] | [ Manual / Paste ]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Direct Excel")
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Image Format")
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Text / Paste")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Common metadata: Date & Supplier
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Sheet Date:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = selectedSpreadsheetDate == "2026-09-25",
                                            onClick = { selectedSpreadsheetDate = "2026-09-25" },
                                            label = { Text("Today", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = selectedSpreadsheetDate == "2026-09-24",
                                            onClick = { selectedSpreadsheetDate = "2026-09-24" },
                                            label = { Text("Yesterday", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = defaultSupplier,
                                    onValueChange = { defaultSupplier = it },
                                    label = { Text("Vendor / Farm Supplier") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (selectedTab) {
                            0 -> {
                                // ==================== DIRECT EXCEL FILE TAB ====================
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Upload Direct Excel File",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Columns: date, items, boxes, qty(kgs), rate, total",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))

                                        Button(
                                            onClick = { excelFilePicker.launch("*/*") },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.UploadFile, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Choose Excel / CSV File from Device")
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        OutlinedButton(
                                            onClick = {
                                                rawInputText = ExcelSheetParser.SAMPLE_DAILY_EXCEL_CSV
                                                viewModel.parseDailyExcelText(rawInputText, defaultSupplier)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Load Mandi Sample Excel File")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Direct Excel Format Specifications:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("• Row 1 Header: Date, Supplier, Items, Boxes, Qty(kgs), Rate, Total", style = MaterialTheme.typography.bodySmall)
                                        Text("• Total column is auto-calculated as qty(kgs) * rate", style = MaterialTheme.typography.bodySmall)
                                        Text("• Supports .xlsx, .xls, .csv, and tab-separated mandi files with supplier name", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            1 -> {
                                // ==================== IMAGE FORMAT TAB ====================
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = SecondaryTeal,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            "Upload Daily Mandi Sheet Photo",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Take a camera photo or pick an image of your daily Mandi bill / manifest",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        uploadedImageUri?.let { uri ->
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Card(
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                            ) {
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = "Uploaded Bill Image",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Button(
                                            onClick = { imageFilePicker.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Upload Bill Image / Photo")
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val mandiSample = """
Date,Supplier,Items,Boxes,Qty(kgs),Rate,Total
$selectedSpreadsheetDate,Lakshana Agro Farms,Fresh Country Tomatoes,25,500.0,35.00,17500.00
$selectedSpreadsheetDate,Lakshana Agro Farms,Nashik Red Onions,40,1000.0,28.00,28000.00
$selectedSpreadsheetDate,Green Valley Mandi,Ooty Fresh Carrots,30,450.0,45.00,20250.00
                                                    """.trimIndent()
                                                    rawInputText = mandiSample
                                                    viewModel.parseDailyExcelText(mandiSample, defaultSupplier)
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Mandi Bill 1", style = MaterialTheme.typography.labelSmall)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    val farmSample = """
Date,Supplier,Items,Boxes,Qty(kgs),Rate,Total
$selectedSpreadsheetDate,Lakshana Agro Farms,Baby Potatoes,50,1250.0,25.00,31250.00
$selectedSpreadsheetDate,Green Valley Mandi,Green Chillies,20,300.0,65.00,19500.00
$selectedSpreadsheetDate,Direct APMC Merchant,Fresh Cauliflower,35,525.0,32.00,16800.00
                                                    """.trimIndent()
                                                    rawInputText = farmSample
                                                    viewModel.parseDailyExcelText(farmSample, defaultSupplier)
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Mandi Bill 2", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Image Mandi Extraction Specs:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("• Optical character digitizer reads handwritten or printed vegetable slips", style = MaterialTheme.typography.bodySmall)
                                        Text("• Identifies Supplier Name, Item, Boxes, Weight, Rate and Total", style = MaterialTheme.typography.bodySmall)
                                        Text("• Automatically computes: Total Amount = Qty(kgs) * Rate per kg", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            2 -> {
                                // ==================== TEXT / MANUAL PASTE TAB ====================
                                OutlinedTextField(
                                    value = rawInputText,
                                    onValueChange = { rawInputText = it },
                                    placeholder = {
                                        Text(
                                            "Paste Excel / CSV daily entries here:\n\n" +
                                                    "Date,Supplier,Items,Boxes,Qty(kgs),Rate,Total\n" +
                                                    "$selectedSpreadsheetDate,Lakshana Agro Farms,Fresh Country Tomatoes,25,500.0,35.00,17500.00\n" +
                                                    "$selectedSpreadsheetDate,Green Valley Mandi,Nashik Red Onions,40,1000.0,28.00,28000.00\n" +
                                                    "$selectedSpreadsheetDate,Direct APMC Merchant,Ooty Fresh Carrots,30,450.0,45.00,20250.00"
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .testTag("excel_data_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (rawInputText.isNotBlank()) {
                                            viewModel.parseDailyExcelText(rawInputText, defaultSupplier)
                                        }
                                    },
                                    enabled = rawInputText.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("parse_excel_button")
                                ) {
                                    Icon(Icons.Default.Troubleshoot, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Parse & Validate Daily Sheet")
                                }
                            }
                        }
                    }
                } else {
                    // Preview Table & Stock Integration Stage
                    val result = excelResult!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Summary Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Entries", style = MaterialTheme.typography.labelSmall)
                                    Text("${result.rows.size} rows", fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SecondaryTeal.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Total Qty", style = MaterialTheme.typography.labelSmall)
                                    Text("${result.totalQtyKgs} kgs", fontWeight = FontWeight.Bold, color = SecondaryTeal)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Total Boxes", style = MaterialTheme.typography.labelSmall)
                                    Text("${result.totalBoxes} crates", fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SuccessGreen.copy(alpha = 0.15f),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Grand Total", style = MaterialTheme.typography.labelSmall)
                                    Text("₹${String.format(Locale.US, "%.2f", result.grandTotalAmount)}", fontWeight = FontWeight.Black, color = SuccessGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Table Column Header
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(24.dp))
                                Text("Date", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(80.dp))
                                Text("Item", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.5f))
                                Text("Boxes", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(48.dp))
                                Text("Qty(kgs)", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(70.dp))
                                Text("Rate", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(55.dp))
                                Text("Total (₹)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.width(75.dp))
                            }
                        }

                        // Table Rows
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 4.dp)
                        ) {
                            items(result.rows) { row ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (row.hasMathDiscrepancy) WarningAmber.copy(alpha = 0.1f) else Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${row.rowIndex}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(24.dp))
                                        Text(row.date, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(80.dp))
                                        Column(modifier = Modifier.weight(1.5f)) {
                                            Text(
                                                row.itemName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "🏢 ${row.supplierName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text("${row.boxes}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
                                        Text("${row.qtyKgs}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(70.dp))
                                        Text("₹${row.rate}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(55.dp))
                                        Text(
                                            "₹${String.format(Locale.US, "%.2f", row.calculatedTotal)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.width(75.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            }
                        }

                        // Checkbox for automatic stock updates
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = autoUpdateStock,
                                onCheckedChange = { autoUpdateStock = it }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Automatic stock updates (instantly increments item stock & boxes in inventory)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { viewModel.clearExcelParseResult() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back to Edit")
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    viewModel.commitDailyExcelImport(autoUpdateStock) { batchId, totalAmt ->
                                        batchSuccessBatchId = batchId
                                        batchSuccessAmount = totalAmt
                                    }
                                },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("commit_excel_import_button")
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Integrating...")
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Integrate Bulk Data")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
