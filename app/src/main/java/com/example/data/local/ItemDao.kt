package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getItemByName(name: String): ItemEntity?

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>): List<Long>

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET currentStockKgs = currentStockKgs + :addKgs, totalBoxes = totalBoxes + :addBoxes, lastUpdated = :now WHERE id = :itemId")
    suspend fun addStock(itemId: Long, addKgs: Double, addBoxes: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE items SET currentStockKgs = :newKgs, totalBoxes = :newBoxes, lastUpdated = :now WHERE id = :itemId")
    suspend fun setStock(itemId: Long, newKgs: Double, newBoxes: Int, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()
}
