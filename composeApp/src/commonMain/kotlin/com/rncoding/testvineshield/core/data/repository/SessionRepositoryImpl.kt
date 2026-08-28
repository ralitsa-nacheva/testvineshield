package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.SessionDao
import com.rncoding.testvineshield.core.data.local.database.entities.SessionEntity
import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthState
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.StorageError
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.security.SecureStorage
import com.rncoding.testvineshield.core.domain.security.SessionPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class SessionRepositoryImpl(
    private val secureStorage: SecureStorage,
    private val json: Json
) : SessionRepository {

    private companion object {
        const val SESSION_KEY = "vineyard.session"
    }

    override suspend fun getSession():
            Result<SessionDomainModel?, AppError> {

        val serialized: String?

        try {
            serialized = secureStorage.get(SESSION_KEY)
        } catch (e: Exception) {
            return Result.Error(
                StorageError.ReadError(e)
            )
        }

        if (serialized == null) {
            return Result.Success(null)
        }

        return try {

            val session =
                json.decodeFromString<SessionDomainModel>(
                    serialized
                )

            Result.Success(session)

        } catch (e: SerializationException) {

            Result.Error(
                StorageError.CorruptedData(e)
            )

        } catch (e: Exception) {

            Result.Error(
                StorageError.ReadError(e)
            )
        }
    }

    override suspend fun saveSession(
        session: SessionDomainModel
    ): Result<Unit, AppError> {

        val serialized: String

        try {

            serialized =
                json.encodeToString(session)

        } catch (e: Exception) {

            return Result.Error(
                StorageError.WriteError(e)
            )
        }

        return try {

            secureStorage.save(
                SESSION_KEY,
                serialized
            )

            Result.Success(Unit)

        } catch (e: Exception) {

            Result.Error(
                StorageError.WriteError(e)
            )
        }
    }

    override suspend fun clearSession():
            Result<Unit, AppError> {

        return try {

            secureStorage.delete(
                SESSION_KEY
            )

            Result.Success(Unit)

        } catch (e: Exception) {

            Result.Error(
                StorageError.DeleteError(e)
            )
        }
    }
}