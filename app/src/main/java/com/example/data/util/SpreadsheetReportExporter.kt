package com.example.data.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.PurchaseEntryEntity
import com.example.data.model.PurchaseStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SpreadsheetReportExporter {

    /**
     * Generates standard RFC-4180 CSV spreadsheet report format suitable for
     * Microsoft Excel, Google Sheets, LibreOffice, and external auditing software.
     */
    fun generateDailySpreadsheetCsv(
        purchases: List<PurchaseEntryEntity>,
        filterDate: String? = null
    ): String {
        val filtered = if (!filterDate.isNullOrBlank() && filterDate != "All") {
            purchases.filter { it.date == filterDate }
        } else {
            purchases
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val sb = StringBuilder()
        // Metadata / Audit Header
        sb.appendLine("\"LAKSHANA VEGGIE EXPRESS - DAILY SPREADSHEET AUDIT REPORT\"")
        sb.appendLine("\"Generated On\",\"$timestamp\",\"Currency\",\"INR (Rs / ₹)\"")
        sb.appendLine("\"Audit Scope\",\"${if (filterDate.isNullOrBlank() || filterDate == "All") "All Dates Ledger" else "Date: $filterDate"}\"")
        sb.appendLine()

        // Standard CSV Columns matching intake & auditing format
        sb.appendLine("Date,Items,Boxes,Qty(kgs),Rate(Rs),Total(Rs),Supplier,BatchId,Status,PaymentRef")

        var totalBoxes = 0
        var totalKgs = 0.0
        var grandTotal = 0.0
        var totalPaid = 0.0
        var totalPending = 0.0

        filtered.forEach { p ->
            totalBoxes += p.boxes
            totalKgs += p.qtyKgs
            grandTotal += p.totalAmount
            if (p.status == PurchaseStatus.PAID || p.status == PurchaseStatus.PAID_CASH || p.status == PurchaseStatus.PAID_UPI) {
                totalPaid += p.totalAmount
            } else {
                totalPending += p.totalAmount
            }

            val escapedItem = escapeCsv(p.itemName)
            val escapedSupplier = escapeCsv(p.supplierName)
            val escapedBatch = escapeCsv(p.batchId.ifEmpty { "PO-#${p.id}" })
            val paymentRef = escapeCsv(p.stripePaymentIntentId ?: if (p.status == PurchaseStatus.PAID_CASH) "CASH_SETTLED" else "")

            sb.appendLine(
                "${p.date}," +
                        "$escapedItem," +
                        "${p.boxes}," +
                        "${String.format(Locale.US, "%.2f", p.qtyKgs)}," +
                        "${String.format(Locale.US, "%.2f", p.rate)}," +
                        "${String.format(Locale.US, "%.2f", p.totalAmount)}," +
                        "$escapedSupplier," +
                        "$escapedBatch," +
                        "${p.status.name}," +
                        paymentRef
            )
        }

        sb.appendLine()
        // Audit Summary Footer
        sb.appendLine(
            "\"AUDIT SUMMARY\"," +
                    "\"Total Records: ${filtered.size}\"," +
                    "$totalBoxes," +
                    "${String.format(Locale.US, "%.2f", totalKgs)}," +
                    "${String.format(Locale.US, "%.2f", if (totalKgs > 0) grandTotal / totalKgs else 0.0)}," +
                    "${String.format(Locale.US, "%.2f", grandTotal)}," +
                    "\"Paid: Rs.${String.format(Locale.US, "%.2f", totalPaid)}\"," +
                    "\"Pending: Rs.${String.format(Locale.US, "%.2f", totalPending)}\"," +
                    "\"AUDIT_VERIFIED\"," +
                    "\"${if (totalPending == 0.0 && filtered.isNotEmpty()) "FULL_SETTLED" else "PENDING_BALANCES"}\""
        )

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /**
     * Writes CSV string directly to the Destination URI selected by user
     * via ActivityResultContracts.CreateDocument("text/csv").
     */
    fun writeCsvToUri(context: Context, uri: Uri, csvContent: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Creates a cached CSV file and returns its FileProvider Uri for external sharing.
     */
    fun createShareableReportFile(context: Context, csvContent: String, fileName: String): Uri? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()
            val file = File(reportsDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(csvContent.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares the CSV report with external auditors, accountants, WhatsApp, Email, or Drive.
     */
    fun shareReport(
        context: Context,
        csvContent: String,
        fileName: String,
        subject: String = "Lakshana Veggie - Daily Spreadsheet Audit Report"
    ) {
        try {
            val fileUri = createShareableReportFile(context, csvContent, fileName)
            val intent = Intent(Intent.ACTION_SEND).apply {
                if (fileUri != null) {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Attached is the Lakshana Veggie Daily Spreadsheet Audit Report for external auditing and record-keeping.\n\n$fileName\n\nPreview:\n" + csvContent.take(500) + "...")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Export / Share Audit Spreadsheet Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
