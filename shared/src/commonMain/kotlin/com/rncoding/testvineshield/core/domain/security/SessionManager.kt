package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.auth.SessionState
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.time.AppClock

/**
 * Coordinates session lifecycle rules.
 */
class SessionManager(
    private val sessionRepository: SessionRepository,
    private val clock: AppClock
) {

    /**
     * Creates and persists a new session for the specified user.
     *
     * The session's maximum lifetime is established here and is represented
     * by expiresAt. Refreshing the session later MUST NOT extend expiresAt.
     */
    suspend fun createSession(
        userId: Long
    ): Result<SessionDomainModel, AppError> {

        val now = clock.nowMillis()

        val session = SessionDomainModel(
            userId = userId,
            createdAt = now,
            lastActiveAt = now,
            expiresAt = now + SessionPolicy.MAX_SESSION_DURATION_MILLIS
        )

        return when (
            val result = sessionRepository.saveSession(session)
        ) {
            is Result.Success -> {
                Result.Success(session)
            }

            is Result.Error -> {
                Result.Error(result.error)
            }
        }
    }

    /**
     * Returns the currently persisted session.
     *
     * Null means that the user has no persisted session.
     */
    suspend fun getSession():
            Result<SessionDomainModel?, AppError> {
        return sessionRepository.getSession()
    }

    /**
     * Determines the current state of the persisted session.
     *
     * Expired sessions are removed from storage immediately.
     */
    suspend fun getSessionState():
            Result<SessionState, AppError> {

        return when (val result = sessionRepository.getSession()) {

            is Result.Error -> {
                Result.Error(result.error)
            }

            is Result.Success -> {

                val session = result.data

                if (session == null) {
                    return Result.Success(SessionState.LoggedOut)
                }

                val now = clock.nowMillis()

                when {
                    now >= session.expiresAt -> {
                        clearExpiredSession()
                    }

                    now - session.lastActiveAt >=
                            SessionPolicy.INACTIVITY_TIMEOUT_MILLIS -> {
                        Result.Success(SessionState.TimedOut)
                    }

                    else -> {
                        Result.Success(SessionState.Active)
                    }
                }
            }
        }
    }

    /**
     * Refreshes the session after the user has successfully authenticated
     * or unlocked the application.
     *
     * IMPORTANT:
     *
     * expiresAt is never changed here.
     *
     * Therefore repeated unlocks cannot keep a session alive forever.
     */
    suspend fun refreshSession():
            Result<SessionDomainModel, AppError> {

        return when (val result = sessionRepository.getSession()) {

            is Result.Error -> {
                Result.Error(result.error)
            }

            is Result.Success -> {

                val session = result.data
                    ?: return Result.Error(AuthError.NoActiveSession)

                val now = clock.nowMillis()

                if (now >= session.expiresAt) {
                    return when (
                        val clearResult = sessionRepository.clearSession()
                    ) {
                        is Result.Success -> {
                            Result.Error(AuthError.SessionExpired)
                        }

                        is Result.Error -> {
                            Result.Error(clearResult.error)
                        }
                    }
                }

                val refreshedSession = session.copy(
                    lastActiveAt = now
                )

                when (
                    val saveResult =
                        sessionRepository.saveSession(refreshedSession)
                ) {
                    is Result.Success -> {
                        Result.Success(refreshedSession)
                    }

                    is Result.Error -> {
                        Result.Error(saveResult.error)
                    }
                }
            }
        }
    }

    /**
     * Removes the current persisted session.
     */
    suspend fun clearSession():
            Result<Unit, AppError> {
        return sessionRepository.clearSession()
    }

    /**
     * Clears a session that has reached its absolute expiration.
     *
     * We do not introduce a new error when clearing succeeds.
     * The caller receives SessionState.Expired.
     *
     * If clearing fails, the storage error is propagated because silently
     * ignoring the failure could leave an expired session persisted.
     */
    private suspend fun clearExpiredSession():
            Result<SessionState, AppError> {

        return when (val result = sessionRepository.clearSession()) {

            is Result.Success -> {
                Result.Success(SessionState.Expired)
            }

            is Result.Error -> {
                Result.Error(result.error)
            }
        }
    }
}