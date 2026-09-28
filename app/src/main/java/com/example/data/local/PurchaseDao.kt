package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PurchaseEntryEntity
import com.example.data.model.PurchaseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY id DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntryEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: Long): PurchaseEntryEntity?

    @Query("SELECT * FROM purchases WHERE batchId = :batchId")
    suspend fun getPurchasesByBatch(batchId: String): List<PurchaseEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<PurchaseEntryEntity>): List<Long>

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntryEntity)

    @Query("UPDATE purchases SET status = :status, stripePaymentIntentId = :paymentIntentId WHERE id = :id")
    suspend fun updateStatus(id: Long, status: PurchaseStatus, paymentIntentId: String?)

    @Query("UPDATE purchases SET status = :status, stripePaymentIntentId = :paymentIntentId, notes = :notes WHERE id = :id")
    suspend fun updateStatusAndNotes(id: Long, status: PurchaseStatus, paymentIntentId: String?, notes: String)

    @Query("UPDATE purchases SET status = :status, stripePaymentIntentId = :paymentIntentId WHERE batchId = :batchId")
    suspend fun updateBatchPaymentStatus(batchId: String, status: PurchaseStatus, paymentIntentId: String?)

    @Delete
    suspend fun deletePurchase(purchase: PurchaseEntryEntity)

    @Query("DELETE FROM purchases")
    suspend fun deleteAllPurchases()
}
