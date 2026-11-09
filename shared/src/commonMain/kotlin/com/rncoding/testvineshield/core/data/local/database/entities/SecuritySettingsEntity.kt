package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "security_settings",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id")
    ]
)
data class SecuritySettingsEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "biometric_enabled")
    val biometricEnabled: Boolean,

    @ColumnInfo(name = "app_pin_enabled")
    val appPinEnabled: Boolean,

    @ColumnInfo(name = "inactivity_lock_enabled", defaultValue = "0")
    val inactivityLockEnabled: Boolean = false,
    @ColumnInfo(name = "inactivity_timeout_millis", defaultValue = "900000")
    val inactivityTimeoutMillis: Long = 900000L,
    @ColumnInfo(name = "device_credential_enabled", defaultValue = "0")
    val deviceCredentialEnabled: Boolean = false
)
