package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.SecuritySettingsDao
import com.rncoding.testvineshield.core.data.local.mappers.SecuritySettingsMapper
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.SecuritySettings
import com.rncoding.testvineshield.core.domain.error.SecurityError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SecuritySettingsRepositoryImpl(
    private val dao: SecuritySettingsDao,
    private val mapper: SecuritySettingsMapper
) : SecuritySettingsRepository {

    override fun observeSettings(
        userId: Long
    ): Flow<SecuritySettings> {

        return dao
            .observeSettings(userId)
            .map { entity ->

                entity?.let(mapper::entityToDomain)
                    ?: SecuritySettings(
                        biometricEnabled = false,
                        appPinEnabled = false
                    )
            }
    }

    override suspend fun getSettings(
        userId: Long
    ): Result<SecuritySettings, AppError> {

        return try {

            val entity =
                dao.getSettings(userId)

            Result.Success(
                entity?.let(mapper::entityToDomain)
                    ?: SecuritySettings(
                        biometricEnabled = false,
                        appPinEnabled = false
                    )
            )

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.READ,
                    cause = e
                )
            )
        }
    }

    override suspend fun saveSettings(
        userId: Long,
        settings: SecuritySettings
    ): Result<Unit, AppError> {

        if (!SecuritySettings.validTimeout(settings.inactivityTimeoutMillis)) {
            return Result.Error(SecurityError.InvalidLockSettings)
        }
        return try {

            dao.upsertSettings(
                mapper.domainToEntity(
                    userId = userId,
                    settings = settings.normalized()
                )
            )

            Result.Success(Unit)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.UPDATE,
                    cause = e
                )
            )
        }
    }

    override suspend fun setBiometricEnabled(userId: Long, enabled: Boolean): Result<Unit, AppError> =
        when (val current = getSettings(userId)) {
            is Result.Error -> current
            is Result.Success -> saveSettings(userId, current.data.copy(biometricEnabled = enabled))
        }

    override suspend fun setPinEnabled(userId: Long, enabled: Boolean): Result<Unit, AppError> =
        when (val current = getSettings(userId)) {
            is Result.Error -> current
            is Result.Success -> if (enabled) Result.Error(SecurityError.UserFallback)
                else saveSettings(userId, current.data.copy(appPinEnabled = false))
        }
}
