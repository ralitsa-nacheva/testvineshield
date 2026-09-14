package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "security_settings"
)
data class SecuritySettingsEntity(

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "biometric_enabled")
    val biometricEnabled: Boolean,

    @ColumnInfo(name = "app_pin_enabled")
    val appPinEnabled: Boolean
)
