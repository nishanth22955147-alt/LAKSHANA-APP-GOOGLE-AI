package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val contactPerson: String,
    val phone: String,
    val email: String,
    val address: String = "",
    val stripeAccountId: String = "",
    val totalPurchasesAmount: Double = 0.0,
    val totalPaidAmount: Double = 0.0,
    val outstandingPayable: Double = 0.0,
    val rating: Float = 4.5f,
    val vegetableCategories: String = "Country Tomatoes, Onions, Potatoes, Greens",
    val paymentUpiId: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
