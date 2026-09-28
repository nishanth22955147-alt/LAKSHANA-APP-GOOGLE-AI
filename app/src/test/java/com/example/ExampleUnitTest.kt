package com.example

import com.example.data.model.ItemEntity
import com.example.data.model.UserRole
import com.example.data.util.ExcelSheetParser
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testExcelParser_parsesValidCsvAndComputesTotal() {
        val testCsv = """
            Date,Items,Boxes,Qty(kgs),Rate,Total
            2026-09-25,Alphonso Mangoes,20,400.0,4.50,1800.00
            2026-09-25,Red Apples,30,600.0,2.80,1680.00
        """.trimIndent()

        val result = ExcelSheetParser.parse(testCsv, "Test Supplier")
        assertTrue(result.success)
        assertEquals(2, result.rows.size)
        assertEquals(50, result.totalBoxes)
        assertEquals(1000.0, result.totalQtyKgs, 0.001)
        assertEquals(3480.0, result.grandTotalAmount, 0.001)

        val row1 = result.rows[0]
        assertEquals("Alphonso Mangoes", row1.itemName)
        assertEquals(20, row1.boxes)
        assertEquals(400.0, row1.qtyKgs, 0.001)
        assertEquals(4.50, row1.rate, 0.001)
        assertEquals(1800.0, row1.calculatedTotal, 0.001)
        assertFalse(row1.hasMathDiscrepancy)
    }

    @Test
    fun testExcelParser_flagsMathDiscrepancy() {
        val testCsvWithErr = """
            Date,Items,Boxes,Qty(kgs),Rate,Total
            2026-09-25,Alphonso Mangoes,10,100.0,5.00,999.00
        """.trimIndent()

        val result = ExcelSheetParser.parse(testCsvWithErr, "Test Supplier")
        assertTrue(result.success)
        val row = result.rows.first()
        assertEquals(500.0, row.calculatedTotal, 0.001)
        assertEquals(999.0, row.enteredTotal)
        assertTrue(row.hasMathDiscrepancy)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun testItemLowStockCalculation() {
        val normalItem = ItemEntity(
            name = "Apples",
            code = "SKU-1",
            category = "Fruits",
            currentStockKgs = 120.0,
            minStockThresholdKgs = 50.0
        )
        assertFalse(normalItem.isLowStock)

        val lowStockItem = ItemEntity(
            name = "Cashews",
            code = "SKU-2",
            category = "Dry Fruits",
            currentStockKgs = 30.0,
            minStockThresholdKgs = 50.0
        )
        assertTrue(lowStockItem.isLowStock)
    }

    @Test
    fun testUserRoleDefinitions() {
        assertEquals("Administrator", UserRole.ADMIN.displayName)
        assertEquals("Operations Manager", UserRole.MANAGER.displayName)
        assertEquals("Purchase Officer", UserRole.PURCHASER.displayName)
    }

    @Test
    fun testPurchaseStatusSettlement() {
        val pending = com.example.data.model.PurchaseStatus.PENDING_PAYMENT
        val paidCash = com.example.data.model.PurchaseStatus.PAID_CASH
        val paidUpi = com.example.data.model.PurchaseStatus.PAID_UPI

        assertFalse(pending.isSettled)
        assertTrue(paidCash.isSettled)
        assertTrue(paidUpi.isSettled)
    }

    @Test
    fun testDateFormatterUtil_ddmmyyForwardAndBackward() {
        val dateUtil = com.example.ui.components.DateFormatterUtil
        // Test parsing of DD-MM-yy
        val cal = dateUtil.parseToCalendar("27-09-26")
        assertEquals("27-09-26", dateUtil.formatDDMMYY(cal))
        assertEquals("270926", dateUtil.formatCompactDDMMYY(cal))

        // Step Backward (-1 Day)
        val calPrev = (cal.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        assertEquals("26-09-26", dateUtil.formatDDMMYY(calPrev))
        assertEquals("260926", dateUtil.formatCompactDDMMYY(calPrev))

        // Step Forward (+1 Day)
        val calNext = (cal.clone() as java.util.Calendar).apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        assertEquals("28-09-26", dateUtil.formatDDMMYY(calNext))
        assertEquals("280926", dateUtil.formatCompactDDMMYY(calNext))
    }

    @Test
    fun testDateFormatterUtil_supportsMultipleDateFormats() {
        val dateUtil = com.example.ui.components.DateFormatterUtil
        
        // Compact DDMMYY
        val calCompact = dateUtil.parseToCalendar("250926")
        assertEquals("25-09-26", dateUtil.formatDDMMYY(calCompact))

        // Slash format DD/MM/YY
        val calSlash = dateUtil.parseToCalendar("25/09/26")
        assertEquals("25-09-26", dateUtil.formatDDMMYY(calSlash))

        // ISO format YYYY-MM-DD
        val calIso = dateUtil.parseToCalendar("2026-09-25")
        assertEquals("25-09-26", dateUtil.formatDDMMYY(calIso))
    }
}
