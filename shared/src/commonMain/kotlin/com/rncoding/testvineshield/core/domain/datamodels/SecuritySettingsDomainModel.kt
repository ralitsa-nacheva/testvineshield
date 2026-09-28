package com.rncoding.testvineshield.core.domain.datamodels

import androidx.room.ColumnInfo

data class SecuritySettingsDomainModel(
    val userId: Long,
    val biometricEnabled: Boolean,
    val appPinEnabled: Boolean
)
