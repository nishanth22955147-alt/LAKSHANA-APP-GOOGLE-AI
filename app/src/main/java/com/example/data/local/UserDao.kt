package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET pinHash = :newPin WHERE id = :userId")
    suspend fun updatePin(userId: Long, newPin: String)

    @Query("UPDATE users SET role = :newRole WHERE id = :userId")
    suspend fun updateUserRole(userId: Long, newRole: UserRole)

    @Query("UPDATE users SET isActive = :isActive WHERE id = :userId")
    suspend fun updateUserStatus(userId: Long, isActive: Boolean)

    @Query("UPDATE users SET lastLoginAt = :timestamp WHERE id = :userId")
    suspend fun updateLastLogin(userId: Long, timestamp: Long)

    @Query("SELECT * FROM users WHERE isApproved = 0 ORDER BY createdAt DESC")
    fun getPendingUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE isApproved = 1 ORDER BY createdAt DESC")
    fun getApprovedUsers(): Flow<List<UserEntity>>

    @Query("UPDATE users SET isApproved = 1, role = :newRole, department = :department, isActive = 1 WHERE id = :userId")
    suspend fun approveUser(userId: Long, newRole: UserRole, department: String)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
