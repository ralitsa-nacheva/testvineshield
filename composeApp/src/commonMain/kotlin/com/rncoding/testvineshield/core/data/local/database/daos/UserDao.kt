package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.relations.UserWithVineyards
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    @Transaction
    suspend fun upsertUser(user: UserEntity): Long // should split this into separate insert and updatepassword functions

    @Insert(onConflict = OnConflictStrategy.REPLACE) // is this correct?
    suspend fun registerUser(user: UserEntity): UserEntity

    @Upsert
    suspend fun changePassword() //todo

    @Query("Select * From user Where user_id = :userId")
    suspend fun getUserById(userId: Long): Flow<UserEntity>

    @Query("SELECT * FROM user WHERE user_email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("Select * From user")
    suspend fun getAllUsers(): Flow<List<UserEntity>>

    @Transaction
    @Query("SELECT * FROM user WHERE user_id = :userId")
    suspend fun getUserWithVineyards(userId: Long): List<UserWithVineyards>

    @Query("Delete From user Where user_id = :userId")
    suspend fun deleteUser(userId: Long)

    @Query("Delete From user")
    suspend fun deleteAllUsers()
}