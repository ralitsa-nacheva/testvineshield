package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.SecuritySettingsDao
import com.rncoding.testvineshield.core.data.local.mappers.SecuritySettingsMapper
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.SecuritySettings
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

        return try {

            dao.upsertSettings(
                mapper.domainToEntity(
                    userId = userId,
                    settings = settings
                )
            )

            Result.Success(Unit)

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.UPDATE,
                    cause = e
                )
            )
        }
    }

    override suspend fun setBiometricEnabled(
        userId: Long,
        enabled: Boolean
    ): Result<Unit, AppError> {

        return try {

            val rowsUpdated =
                dao.setBiometricEnabled(
                    userId = userId,
                    enabled = enabled
                )

            if (rowsUpdated == 0) {

                /*
                 * Settings don't exist yet.
                 * Create them with the requested value.
                 */
                val current =
                    dao.getSettings(userId)

                val settings =
                    current?.let(mapper::entityToDomain)
                        ?: SecuritySettings(
                            biometricEnabled = false,
                            appPinEnabled = false
                        )

                dao.upsertSettings(
                    mapper.domainToEntity(
                        userId = userId,
                        settings = settings.copy(
                            biometricEnabled = enabled
                        )
                    )
                )
            }

            Result.Success(Unit)

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.UPDATE,
                    cause = e
                )
            )
        }
    }

    override suspend fun setPinEnabled(
        userId: Long,
        enabled: Boolean
    ): Result<Unit, AppError> {

        return try {

            val rowsUpdated =
                dao.setPinEnabled(
                    userId = userId,
                    enabled = enabled
                )

            if (rowsUpdated == 0) {

                val current =
                    dao.getSettings(userId)

                val settings =
                    current?.let(mapper::entityToDomain)
                        ?: SecuritySettings(
                            biometricEnabled = false,
                            appPinEnabled = false
                        )

                dao.upsertSettings(
                    mapper.domainToEntity(
                        userId = userId,
                        settings = settings.copy(
                            appPinEnabled = enabled
                        )
                    )
                )
            }

            Result.Success(Unit)

        } catch (e: Exception) {

            Result.Error(
                DatabaseError.Unknown(
                    operation = DatabaseError.Operation.UPDATE,
                    cause = e
                )
            )
        }
    }
}