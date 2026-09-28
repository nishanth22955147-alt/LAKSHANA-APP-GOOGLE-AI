package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Query("SELECT * FROM suppliers WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getSupplierByName(name: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>): List<Long>

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET outstandingPayable = outstandingPayable + :amount, totalPurchasesAmount = totalPurchasesAmount + :amount WHERE id = :id")
    suspend fun addPayable(id: Long, amount: Double)

    @Query("UPDATE suppliers SET outstandingPayable = MAX(0.0, outstandingPayable - :amount), totalPaidAmount = totalPaidAmount + :amount WHERE id = :id")
    suspend fun recordPayment(id: Long, amount: Double)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers")
    suspend fun deleteAllSuppliers()
}
