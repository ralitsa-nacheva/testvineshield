package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.StorageError
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.security.SecureStorage
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

        /*
         * Step 1:
         * Read the serialized session from secure storage.
         */
        try {

            serialized =
                secureStorage.get(SESSION_KEY)

        } catch (e: Exception) {

            return Result.Error(
                StorageError.ReadError(
                    cause = e
                )
            )
        }

        /*
         * No stored session means the user is not logged in.
         *
         * This is NOT an error.
         */
        if (serialized == null) {
            return Result.Success(null)
        }

        /*
         * Step 2:
         * Deserialize the stored session.
         */
        return try {

            val session =
                json.decodeFromString<SessionDomainModel>(
                    serialized
                )

            Result.Success(session)

        } catch (e: SerializationException) {

            /*
             * The storage operation succeeded,
             * but the stored data is invalid.
             */
            Result.Error(
                StorageError.CorruptedData(
                    cause = e
                )
            )

        } catch (e: Exception) {

            /*
             * Unexpected storage/serialization failure.
             */
            Result.Error(
                StorageError.ReadError(
                    cause = e
                )
            )
        }
    }

    override suspend fun saveSession(
        session: SessionDomainModel
    ): Result<Unit, AppError> {

        /*
         * Step 1:
         * Serialize the domain model.
         */
        val serialized: String

        try {

            serialized =
                json.encodeToString(session)

        } catch (e: Exception) {

            return Result.Error(
                StorageError.WriteError(
                    cause = e
                )
            )
        }

        /*
         * Step 2:
         * Persist serialized session securely.
         */
        return try {

            secureStorage.save(
                SESSION_KEY,
                serialized
            )

            Result.Success(Unit)

        } catch (e: Exception) {

            Result.Error(
                StorageError.WriteError(
                    cause = e
                )
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
                StorageError.DeleteError(
                    cause = e
                )
            )
        }
    }
}