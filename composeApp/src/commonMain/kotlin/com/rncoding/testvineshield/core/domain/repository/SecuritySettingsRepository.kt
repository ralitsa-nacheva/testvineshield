package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.SecuritySettings
import com.rncoding.testvineshield.core.domain.error.AppError
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.domain.error.Result

interface SecuritySettingsRepository {

    fun observeSettings(
        userId: Long
    ): Flow<SecuritySettings>

    suspend fun getSettings(
        userId: Long
    ): Result<SecuritySettings, AppError>

    suspend fun setBiometricEnabled(
        userId: Long,
        enabled: Boolean
    ): Result<Unit, AppError>

    suspend fun setPinEnabled(
        userId: Long,
        enabled: Boolean
    ): Result<Unit, AppError>
}