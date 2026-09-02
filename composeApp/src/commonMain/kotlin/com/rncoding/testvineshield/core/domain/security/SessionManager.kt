package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.auth.SessionState
import com.rncoding.testvineshield.core.domain.time.SystemClock

class SessionManager(
    private val sessionRepository: SessionRepository,
    private val clock: SystemClock
) {

    suspend fun createSession(
        userId: Long
    ): Result<SessionDomainModel, AppError> {

        val now =
            clock.now()

        val session =
            SessionDomainModel(
                userId = userId,
                createdAt = now,
                lastActiveAt = now,
                expiresAt =
                    now +
                            SessionPolicy.MAX_SESSION_DURATION
            )

        return when (
            val result =
                sessionRepository.saveSession(session)
        ) {

            is Result.Success ->
                Result.Success(session)

            is Result.Error ->
                Result.Error(result.error)
        }
    }

    suspend fun getSession():
            Result<SessionDomainModel?, AppError> {

        return sessionRepository.getSession()
    }

    suspend fun getSessionState():
            Result<SessionState, AppError> {

        return when (
            val result =
                sessionRepository.getSession()
        ) {

            is Result.Error ->
                Result.Error(result.error)

            is Result.Success -> {

                val session =
                    result.data

                if (session == null) {
                    return Result.Success(
                        SessionState.LoggedOut
                    )
                }

                val now =
                    clock.now()

                /*
                 * Absolute expiration.
                 */
                if (now >= session.expiresAt) {

                    when (
                        val clearResult =
                            sessionRepository.clearSession()
                    ) {

                        is Result.Error ->
                            Result.Error(
                                clearResult.error
                            )

                        is Result.Success ->
                            Result.Success(
                                SessionState.Expired
                            )
                    }

                } else {

                    val inactivity =
                        now - session.lastActiveAt

                    when {

                        inactivity >=
                                SessionPolicy.INACTIVITY_TIMEOUT -> {

                            Result.Success(
                                SessionState.TimedOut
                            )
                        }

                        inactivity >=
                                SessionPolicy.REAUTH_THRESHOLD -> {

                            Result.Success(
                                SessionState.RequiresReauth
                            )
                        }

                        else -> {

                            Result.Success(
                                SessionState.Active
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun refreshSession():
            Result<SessionDomainModel, AppError> {

        return when (
            val result =
                sessionRepository.getSession()
        ) {

            is Result.Error ->
                Result.Error(result.error)

            is Result.Success -> {

                val session =
                    result.data
                        ?: return Result.Error(
                            AuthError.NoActiveSession
                        )

                val now =
                    clock.now()

                /*
                 * Don't revive an absolutely expired session.
                 */
                if (now >= session.expiresAt) {

                    sessionRepository.clearSession()

                    return Result.Error(
                        AuthError.SessionExpired
                    )
                }

                val refreshed =
                    session.copy(
                        lastActiveAt = now
                    )

                when (
                    val saveResult =
                        sessionRepository.saveSession(
                            refreshed
                        )
                ) {

                    is Result.Error ->
                        Result.Error(
                            saveResult.error
                        )

                    is Result.Success ->
                        Result.Success(
                            refreshed
                        )
                }
            }
        }
    }

    suspend fun updateActivity():
            Result<Unit, AppError> {

        return when (
            val result =
                sessionRepository.getSession()
        ) {

            is Result.Error ->
                Result.Error(result.error)

            is Result.Success -> {

                val session =
                    result.data
                        ?: return Result.Success(Unit)

                val now =
                    clock.now()

                /*
                 * Do not update an already-expired session.
                 */
                if (now >= session.expiresAt) {

                    sessionRepository.clearSession()

                    return Result.Error(
                        AuthError.SessionExpired
                    )
                }

                val updated =
                    session.copy(
                        lastActiveAt = now
                    )

                sessionRepository.saveSession(
                    updated
                )
            }
        }
    }

    suspend fun clearSession():
            Result<Unit, AppError> {

        return sessionRepository.clearSession()
    }
}