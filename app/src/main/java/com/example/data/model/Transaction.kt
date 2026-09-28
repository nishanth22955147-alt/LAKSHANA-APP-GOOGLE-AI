package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionStatus(val label: String) {
    SUCCEEDED("Succeeded"),
    PROCESSING("Processing"),
    FAILED("Failed"),
    REFUNDED("Refunded")
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stripePaymentIntentId: String, // e.g. "pi_3O123456789abcdef"
    val purchaseId: Long? = null,
    val purchaseBatchId: String? = null,
    val supplierName: String,
    val amount: Double,
    val currency: String = "USD",
    val paymentMethod: String = "Stripe Card (Visa •••• 4242)",
    val status: TransactionStatus = TransactionStatus.SUCCEEDED,
    val clientSecret: String? = null,
    val receiptUrl: String? = null,
    val failureMessage: String? = null,
    val processedBy: String = "Admin",
    val timestamp: Long = System.currentTimeMillis()
)
