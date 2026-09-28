package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MovementType(val title: String) {
    EXCEL_BULK_IMPORT("Excel Bulk Inward"),
    MANUAL_PURCHASE("Manual PO Inward"),
    ADJUSTMENT("Audit / Adjustment"),
    STOCK_OUT("Production / Outward")
}

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemName: String,
    val movementType: MovementType,
    val qtyKgsChange: Double, // positive for inward, negative for outward
    val boxesChange: Int,
    val resultingStockKgs: Double,
    val referenceBatchOrPo: String = "",
    val reasonOrNotes: String = "",
    val recordedBy: String = "Admin",
    val timestamp: Long = System.currentTimeMillis()
)
