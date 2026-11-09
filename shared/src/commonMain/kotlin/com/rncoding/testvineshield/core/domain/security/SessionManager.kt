package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.auth.SessionState
import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes activity, expiry, unlock and logout writes. Unlock never extends absolute lifetime. */
class SessionManager(
    private val sessionRepository: SessionRepository,
    private val clock: AppClock,
    private val securitySettingsRepository: SecuritySettingsRepository
) {
    private val mutex = Mutex()

    suspend fun createSession(userId: Long): Result<SessionDomainModel, AppError> = mutex.withLock {
        val now = clock.nowMillis()
        val session = SessionDomainModel(userId, now, now, now + SessionPolicy.MAX_SESSION_DURATION_MILLIS)
        when (val saved = sessionRepository.saveSession(session)) {
            is Result.Error -> Result.Error(saved.error)
            is Result.Success -> Result.Success(session)
        }
    }

    suspend fun getSession(): Result<SessionDomainModel?, AppError> = sessionRepository.getSession()

    suspend fun getSessionState(): Result<SessionState, AppError> = mutex.withLock { stateUnlocked() }

    private suspend fun stateUnlocked(): Result<SessionState, AppError> {
        val session = when (val stored = sessionRepository.getSession()) {
            is Result.Error -> return Result.Error(stored.error)
            is Result.Success -> stored.data ?: return Result.Success(SessionState.LoggedOut)
        }
        val now = clock.nowMillis()
        if (now >= session.expiresAt) return when (val cleared = sessionRepository.clearSession()) {
            is Result.Error -> Result.Error(cleared.error)
            is Result.Success -> Result.Success(SessionState.Expired)
        }
        val settings = when (val saved = securitySettingsRepository.getSettings(session.userId)) {
            is Result.Error -> return Result.Error(saved.error)
            is Result.Success -> saved.data
        }
        if (!SecuritySettings.validTimeout(settings.inactivityTimeoutMillis))
            return Result.Error(SecurityError.InvalidLockSettings)
        return Result.Success(
            if (settings.inactivityLockEnabled && (now < session.lastActiveAt || now - session.lastActiveAt >= settings.inactivityTimeoutMillis))
                SessionState.TimedOut else SessionState.Active)
    }

    /** Call only after explicit successful authentication. */
    suspend fun refreshSession(expectedSession: SessionDomainModel? = null): Result<SessionDomainModel, AppError> = mutex.withLock {
        val session = when (val stored = sessionRepository.getSession()) {
            is Result.Error -> return@withLock Result.Error(stored.error)
            is Result.Success -> stored.data ?: return@withLock Result.Error(AuthError.NoActiveSession)
        }
        if (expectedSession != null && session != expectedSession)
            return@withLock Result.Error(AuthError.NoActiveSession)
        val now = clock.nowMillis()
        if (now >= session.expiresAt) return@withLock when (val cleared = sessionRepository.clearSession()) {
            is Result.Error -> Result.Error(cleared.error)
            is Result.Success -> Result.Error(AuthError.SessionExpired)
        }
        val refreshed = session.copy(lastActiveAt = now)
        when (val saved = sessionRepository.saveSession(refreshed)) {
            is Result.Error -> Result.Error(saved.error)
            is Result.Success -> Result.Success(refreshed)
        }
    }

    /** Genuine UI input only. An event after the deadline cannot unlock a session. */
    suspend fun recordActivity(at: Long): Result<Unit, AppError> = mutex.withLock {
        when (val state = stateUnlocked()) {
            is Result.Error -> return@withLock Result.Error(state.error)
            is Result.Success -> if (state.data != SessionState.Active)
                return@withLock Result.Error(AuthError.UnlockFailed)
        }
        val session = when (val stored = sessionRepository.getSession()) {
            is Result.Error -> return@withLock Result.Error(stored.error)
            is Result.Success -> stored.data ?: return@withLock Result.Error(AuthError.NoActiveSession)
        }
        val timestamp = minOf(at, clock.nowMillis())
        if (timestamp <= session.lastActiveAt) Result.Success(Unit)
        else sessionRepository.saveSession(session.copy(lastActiveAt = timestamp))
    }

    suspend fun clearSession(): Result<Unit, AppError> = mutex.withLock { sessionRepository.clearSession() }
}
