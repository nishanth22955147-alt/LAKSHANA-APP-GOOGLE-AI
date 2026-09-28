package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val displayName: String, val description: String) {
    ADMIN("Administrator", "Full system access: User roles, payments, reports, inventory & bulk uploads"),
    MANAGER("Operations Manager", "Can approve purchases, process Stripe payments, and view all reports"),
    PURCHASER("Purchase Officer", "Can record purchases, upload daily excel sheets, and initiate payments"),
    INVENTORY_CLERK("Inventory Clerk", "Can update stock counts, view items, and log adjustments"),
    VIEWER("Auditor / Viewer", "Read-only access to transactions, inventory, and analytics")
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mobileNumber: String,
    val fullName: String,
    val role: UserRole = UserRole.PURCHASER,
    val pinHash: String = "5147",
    val isActive: Boolean = true,
    val isApproved: Boolean = true, // False for self-registered users awaiting Admin approval
    val department: String = "Procurement",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
