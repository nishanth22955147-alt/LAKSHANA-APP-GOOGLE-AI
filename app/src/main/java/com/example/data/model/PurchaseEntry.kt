package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PurchaseStatus(val label: String) {
    PENDING_PAYMENT("Pending Payment"),
    PAID_CASH("Paid (Cash)"),
    PAID_UPI("Paid (UPI)"),
    PAID("Paid"),
    RECEIVED("Received & Stock Updated"),
    CANCELLED("Cancelled");

    val isSettled: Boolean
        get() = this == PAID_CASH || this == PAID_UPI || this == PAID
}

@Entity(tableName = "purchases")
data class PurchaseEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: String = "", // e.g. "EXCEL-2026-09-25-01" or "PO-1001"
    val date: String, // e.g. "2026-09-25"
    val itemName: String,
    val boxes: Int,
    val qtyKgs: Double,
    val rate: Double,
    val totalAmount: Double, // calculated: qtyKgs * rate
    val supplierId: Long? = null,
    val supplierName: String = "Direct Farm / Vendor",
    val status: PurchaseStatus = PurchaseStatus.PENDING_PAYMENT,
    val paymentMethod: String = "Cash", // "Cash", "UPI (Google Pay)", "UPI (PhonePe)", etc.
    val stripePaymentIntentId: String? = null, // Stores Cash receipt / UPI UTR reference
    val notes: String = "",
    val isExcelImport: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
