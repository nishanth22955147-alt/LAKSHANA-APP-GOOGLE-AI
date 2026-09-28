package com.example.data.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyPurchaseRow(
    val rowIndex: Int,
    val date: String,
    val itemName: String,
    val boxes: Int,
    val qtyKgs: Double,
    val rate: Double,
    val calculatedTotal: Double,
    val enteredTotal: Double?,
    val hasMathDiscrepancy: Boolean = false,
    val supplierName: String = "Wholesale Supplier"
)

data class ExcelParseResult(
    val success: Boolean,
    val rows: List<DailyPurchaseRow>,
    val totalBoxes: Int,
    val totalQtyKgs: Double,
    val grandTotalAmount: Double,
    val warnings: List<String>,
    val errors: List<String>
)

object ExcelSheetParser {

    val SAMPLE_DAILY_EXCEL_CSV = """
Date,Supplier,Items,Boxes,Qty(kgs),Rate,Total
2026-09-25,Lakshana Agro Farms,Fresh Country Tomatoes,25,500.0,35.00,17500.00
2026-09-25,Lakshana Agro Farms,Nashik Red Onions,40,1000.0,28.00,28000.00
2026-09-25,Green Valley Mandi,Ooty Fresh Carrots,30,450.0,45.00,20250.00
2026-09-25,Green Valley Mandi,Green Capsicum,15,225.0,55.00,12375.00
2026-09-25,Direct APMC Merchant,Fresh Cauliflower,35,525.0,32.00,16800.00
    """.trimIndent()

    val SAMPLE_GRAINS_EXCEL_TSV = """
Date	Supplier	Items	Boxes	Qty(kgs)	Rate	Total
2026-09-25	Lakshana Agro Farms	Baby Potatoes	50	1250.0	25.00	31250.00
2026-09-25	Lakshana Agro Farms	Green Chillies	20	300.0	65.00	19500.00
2026-09-25	Green Valley Mandi	Fresh Ginger	10	150.0	110.00	16500.00
2026-09-25	Green Valley Mandi	Fresh Coriander Leaves	20	200.0	40.00	8000.00
    """.trimIndent()

    fun parse(rawText: String, defaultSupplier: String = "Lakshana Agro Farms"): ExcelParseResult {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return ExcelParseResult(
                success = false,
                rows = emptyList(),
                totalBoxes = 0,
                totalQtyKgs = 0.0,
                grandTotalAmount = 0.0,
                warnings = emptyList(),
                errors = listOf("The provided sheet is empty. Please paste or upload valid daily sheet rows.")
            )
        }

        val rows = mutableListOf<DailyPurchaseRow>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        var dateIdx = 0
        var supplierIdx = -1
        var itemIdx = 1
        var boxesIdx = 2
        var qtyIdx = 3
        var rateIdx = 4
        var totalIdx = 5

        // Determine if first row is a header
        val firstLine = lines.first()
        val delimiter = if (firstLine.contains("\t")) "\t" else if (firstLine.contains(",")) "," else ";"
        val headerTokens = splitLine(firstLine, delimiter).map { it.lowercase().trim() }

        val hasHeader = headerTokens.any { 
            it.contains("date") || it.contains("supplier") || it.contains("vendor") || it.contains("farm") ||
                    it.contains("item") || it.contains("box") || it.contains("qty") || it.contains("rate") || it.contains("total")
        }

        var startLineIndex = 0
        if (hasHeader) {
            startLineIndex = 1
            headerTokens.forEachIndexed { index, token ->
                when {
                    token.contains("date") -> dateIdx = index
                    token.contains("supplier") || token.contains("vendor") || token.contains("farm") || token.contains("merchant") -> supplierIdx = index
                    token.contains("item") || token.contains("product") || token.contains("vegetable") || token.contains("name") -> itemIdx = index
                    token.contains("box") || token.contains("crate") || token.contains("bag") -> boxesIdx = index
                    token.contains("qty") || token.contains("kgs") || token.contains("kg") || token.contains("weight") -> qtyIdx = index
                    token.contains("rate") || token.contains("price") -> rateIdx = index
                    token.contains("total") || token.contains("amount") -> totalIdx = index
                }
            }
        } else {
            // Default 7 columns: Date, Supplier, Items, Boxes, Qty(kgs), Rate, Total
            val sampleTokens = splitLine(firstLine, delimiter)
            if (sampleTokens.size >= 7) {
                dateIdx = 0
                supplierIdx = 1
                itemIdx = 2
                boxesIdx = 3
                qtyIdx = 4
                rateIdx = 5
                totalIdx = 6
            }
        }

        for (i in startLineIndex until lines.size) {
            val line = lines[i]
            val tokens = splitLine(line, delimiter)

            if (tokens.size < 4) {
                warnings.add("Line ${i + 1} was skipped due to insufficient columns: '$line'")
                continue
            }

            try {
                val rawDate = tokens.getOrNull(dateIdx)?.trim().orEmpty()
                val date = if (rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                    rawDate
                } else if (rawDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
                    // convert DD/MM/YYYY to YYYY-MM-DD
                    val parts = rawDate.split("/")
                    "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
                } else {
                    todayDate
                }

                // If supplierIdx was not explicitly set but row has >= 7 columns, default column 1 is supplier
                val effectiveSupplierIdx = if (supplierIdx >= 0) supplierIdx else if (tokens.size >= 7) 1 else -1
                val effectiveItemIdx = if (itemIdx >= 0 && itemIdx != effectiveSupplierIdx) itemIdx else if (tokens.size >= 7) 2 else 1

                val rowSupplier = if (effectiveSupplierIdx >= 0) {
                    val sup = tokens.getOrNull(effectiveSupplierIdx)?.trim().orEmpty()
                    if (sup.isNotBlank()) sup else defaultSupplier
                } else {
                    defaultSupplier
                }

                val itemName = tokens.getOrNull(effectiveItemIdx)?.trim()
                if (itemName.isNullOrBlank()) {
                    warnings.add("Line ${i + 1}: Missing item name; skipped.")
                    continue
                }

                val effectiveBoxesIdx = if (boxesIdx >= 0 && boxesIdx != effectiveSupplierIdx && boxesIdx != effectiveItemIdx) {
                    boxesIdx
                } else if (tokens.size >= 7) 3 else 2

                val effectiveQtyIdx = if (qtyIdx >= 0 && qtyIdx != effectiveSupplierIdx && qtyIdx != effectiveItemIdx && qtyIdx != effectiveBoxesIdx) {
                    qtyIdx
                } else if (tokens.size >= 7) 4 else 3

                val effectiveRateIdx = if (rateIdx >= 0 && rateIdx != effectiveSupplierIdx && rateIdx != effectiveItemIdx && rateIdx != effectiveBoxesIdx && rateIdx != effectiveQtyIdx) {
                    rateIdx
                } else if (tokens.size >= 7) 5 else 4

                val effectiveTotalIdx = if (totalIdx >= 0 && totalIdx != effectiveSupplierIdx && totalIdx != effectiveItemIdx && totalIdx != effectiveBoxesIdx && totalIdx != effectiveQtyIdx && totalIdx != effectiveRateIdx) {
                    totalIdx
                } else if (tokens.size >= 7) 6 else 5

                val boxesStr = tokens.getOrNull(effectiveBoxesIdx)?.replace(Regex("[^0-9]"), "") ?: "0"
                val boxes = boxesStr.toIntOrNull() ?: 1

                val qtyStr = tokens.getOrNull(effectiveQtyIdx)?.replace(Regex("[^0-9.]"), "") ?: "0"
                val qtyKgs = qtyStr.toDoubleOrNull() ?: 0.0

                val rateStr = tokens.getOrNull(effectiveRateIdx)?.replace(Regex("[^0-9.]"), "") ?: "0"
                val rate = rateStr.toDoubleOrNull() ?: 0.0

                val totalStr = tokens.getOrNull(effectiveTotalIdx)?.replace(Regex("[^0-9.]"), "")
                val enteredTotal = totalStr?.toDoubleOrNull()

                val calculatedTotal = Math.round(qtyKgs * rate * 100.0) / 100.0

                var hasMathDiscrepancy = false
                if (enteredTotal != null && Math.abs(enteredTotal - calculatedTotal) > 0.05) {
                    hasMathDiscrepancy = true
                    warnings.add("Line ${i + 1} ($itemName): Entered total (₹$enteredTotal) differs from qty * rate (₹$calculatedTotal). Auto-adjusted to computed total.")
                }

                rows.add(
                    DailyPurchaseRow(
                        rowIndex = i + 1,
                        date = date,
                        itemName = itemName,
                        boxes = boxes,
                        qtyKgs = qtyKgs,
                        rate = rate,
                        calculatedTotal = calculatedTotal,
                        enteredTotal = enteredTotal,
                        hasMathDiscrepancy = hasMathDiscrepancy,
                        supplierName = rowSupplier
                    )
                )
            } catch (e: Exception) {
                errors.add("Error parsing line ${i + 1}: ${e.localizedMessage}")
            }
        }

        if (rows.isEmpty()) {
            return ExcelParseResult(
                success = false,
                rows = emptyList(),
                totalBoxes = 0,
                totalQtyKgs = 0.0,
                grandTotalAmount = 0.0,
                warnings = warnings,
                errors = if (errors.isEmpty()) listOf("No valid purchase records could be extracted.") else errors
            )
        }

        val totalBoxes = rows.sumOf { it.boxes }
        val totalQtyKgs = rows.sumOf { it.qtyKgs }
        val grandTotal = rows.sumOf { it.calculatedTotal }

        return ExcelParseResult(
            success = true,
            rows = rows,
            totalBoxes = totalBoxes,
            totalQtyKgs = totalQtyKgs,
            grandTotalAmount = grandTotal,
            warnings = warnings,
            errors = errors
        )
    }

    private fun splitLine(line: String, delimiter: String): List<String> {
        return if (delimiter == "\t") {
            line.split("\t")
        } else {
            // handle commas inside quotes
            val tokens = mutableListOf<String>()
            val sb = java.lang.StringBuilder()
            var inQuotes = false
            for (ch in line) {
                if (ch == '\"') {
                    inQuotes = !inQuotes
                } else if (ch == ',' && !inQuotes) {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                } else {
                    sb.append(ch)
                }
            }
            tokens.add(sb.toString().trim())
            tokens
        }
    }
}
