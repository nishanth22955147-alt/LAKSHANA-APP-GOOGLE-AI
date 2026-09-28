package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String, // e.g. "SKU-APL-01"
    val category: String, // "Fruits", "Vegetables", "Grains", "Raw Materials", "Packaging"
    val currentStockKgs: Double = 0.0,
    val totalBoxes: Int = 0,
    val minStockThresholdKgs: Double = 50.0,
    val defaultRatePerKg: Double = 0.0,
    val unit: String = "kgs",
    val description: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = currentStockKgs <= minStockThresholdKgs
}
