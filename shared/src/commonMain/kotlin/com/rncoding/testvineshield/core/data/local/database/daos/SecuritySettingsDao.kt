package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rncoding.testvineshield.core.data.local.database.entities.SecuritySettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecuritySettingsDao {

    @Query(
        """
        SELECT *
        FROM security_settings
        WHERE user_id = :userId
        LIMIT 1
        """
    )
    suspend fun getSettings(
        userId: Long
    ): SecuritySettingsEntity?

    @Query(
        """
        SELECT *
        FROM security_settings
        WHERE user_id = :userId
        LIMIT 1
        """
    )
    fun observeSettings(
        userId: Long
    ): Flow<SecuritySettingsEntity?>

    @Upsert
    suspend fun upsertSettings(
        settings: SecuritySettingsEntity
    )

    @Query(
        """
        UPDATE security_settings
        SET biometric_enabled = :enabled
        WHERE user_id = :userId
        """
    )
    suspend fun setBiometricEnabled( //maybe use a single method saveSettings that does an upsert transaction
        userId: Long,
        enabled: Boolean
    ): Int

    @Query(
        """
        UPDATE security_settings
        SET app_pin_enabled = :enabled
        WHERE user_id = :userId
        """
    )
    suspend fun setPinEnabled(
        userId: Long,
        enabled: Boolean
    ): Int

    @Query(
        """
        DELETE FROM security_settings
        WHERE user_id = :userId
        """
    )
    suspend fun deleteSettings(
        userId: Long
    ): Int
}