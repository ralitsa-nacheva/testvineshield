package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.AuthState
import com.rncoding.testvineshield.core.domain.error.AppError
import kotlinx.coroutines.flow.Flow

interface SessionRepository {

    suspend fun getSession():
            Result<SessionDomainModel?, AppError>

    suspend fun saveSession(
        session: SessionDomainModel
    ): Result<Unit, AppError>

    suspend fun clearSession():
            Result<Unit, AppError>
}