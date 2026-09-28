package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rncoding.testvineshield.core.data.local.database.entities.UserEntity
import com.rncoding.testvineshield.core.data.local.database.relations.UserWithVineyards

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity): Long

    @Query(
        """
        SELECT *
        FROM user
        WHERE user_id = :userId
        LIMIT 1
        """
    )
    suspend fun getUserById(
        userId: Long
    ): UserEntity?

    @Query(
        """
        SELECT *
        FROM user
        WHERE user_email = :email
        LIMIT 1
        """
    )
    suspend fun getUserByEmail(
        email: String
    ): UserEntity?

    @Query("""
    DELETE FROM user
    WHERE user_id = :userId
""")
    suspend fun deleteUser(
        userId: Long
    ): Int

    @Query(
        """
        DELETE FROM user
        """
    )
    suspend fun deleteAllUsers(): Int

    @Transaction
    @Query("SELECT * FROM user WHERE user_id = :userId")
    suspend fun getUserWithVineyards(userId: Long): List<UserWithVineyards>
    @Query(
        """
    UPDATE user
    SET user_email = :email,
        pass_hash = :passwordHash,
        pass_salt = :passwordSalt
    WHERE user_id = :userId
    """
    )
    suspend fun updateUser(
        userId: Long,
        email: String,
        passwordHash: String,
        passwordSalt: String
    ): Int
}
