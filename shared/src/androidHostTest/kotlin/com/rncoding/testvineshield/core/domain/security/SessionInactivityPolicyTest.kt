package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.auth.SessionState
import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.*

class SessionInactivityPolicyTest {
    private var now = 60_001L
    private var session: SessionDomainModel? = SessionDomainModel(1, 1, 1, 1_000_000)
    private var policy = SecuritySettings()
    private val storage = mockk<SessionRepository> {
        coEvery { getSession() } answers { Result.Success(session) }
        coEvery { saveSession(any()) } answers { session = firstArg(); Result.Success(Unit) }
        coEvery { clearSession() } answers { session = null; Result.Success(Unit) }
    }
    private val settings = mockk<SecuritySettingsRepository> {
        coEvery { getSettings(1) } answers { Result.Success(policy) }
    }
    private val clock = mockk<AppClock> { every { nowMillis() } answers { now } }
    private val manager = SessionManager(storage, clock, settings)
    private suspend fun state() = (manager.getSessionState() as Result.Success).data

    @Test fun defaultOffIgnoresOldInactivity() = runTest { assertEquals(SessionState.Active, state()) }
    @Test fun enabledLocksAtExactDeadline() = runTest {
        policy = SecuritySettings(inactivityLockEnabled = true, inactivityTimeoutMillis = 60_000)
        now = 60_000; assertEquals(SessionState.Active, state())
        now = 60_001; assertEquals(SessionState.TimedOut, state())
    }
    @Test fun configuredTimeoutIsUsed() = runTest {
        policy = SecuritySettings(inactivityLockEnabled = true, inactivityTimeoutMillis = 120_000)
        assertEquals(SessionState.Active, state()); now = 120_001; assertEquals(SessionState.TimedOut, state())
    }
    @Test fun genuineActivityMovesDeadlineWithoutExtendingExpiry() = runTest {
        policy = SecuritySettings(inactivityLockEnabled = true, inactivityTimeoutMillis = 60_000)
        now = 30_000; assertIs<Result.Success<Unit>>(manager.recordActivity(now))
        assertEquals(1_000_000L, session?.expiresAt)
        now = 60_001; assertEquals(SessionState.Active, state())
        now = 90_000; assertEquals(SessionState.TimedOut, state())
    }
    @Test fun lateActivityCannotUnlock() = runTest {
        policy = SecuritySettings(inactivityLockEnabled = true, inactivityTimeoutMillis = 60_000)
        assertIs<Result.Error<AppError>>(manager.recordActivity(now)); assertEquals(1L, session?.lastActiveAt)
    }
    @Test fun expiryStillClearsWhenLockOff() = runTest {
        now = 1_000_000; assertEquals(SessionState.Expired, state()); assertNull(session)
    }
    @Test fun explicitUnlockRefreshPreservesAbsoluteExpiry() = runTest {
        val result = assertIs<Result.Success<SessionDomainModel>>(manager.refreshSession())
        assertEquals(now, result.data.lastActiveAt); assertEquals(1_000_000L, result.data.expiresAt)
    }
    @Test fun oldQueuedActivityCannotResurrectLogout() = runTest {
        manager.clearSession(); assertIs<Result.Error<AppError>>(manager.recordActivity(now)); assertNull(session)
    }
    @Test fun storagePolicyErrorIsNotTreatedAsOff() = runTest {
        coEvery { settings.getSettings(1) } returns Result.Error(UnexpectedError())
        assertIs<Result.Error<AppError>>(manager.getSessionState())
    }
    @Test fun invalidTimeoutIsRejected() = runTest {
        policy = SecuritySettings(inactivityLockEnabled = true, inactivityTimeoutMillis = 0)
        assertIs<Result.Error<AppError>>(manager.getSessionState())
    }
    @Test fun backwardClockDoesNotMakeEnabledLockStayActive() = runTest {
        policy = SecuritySettings(inactivityLockEnabled = true, inactivityTimeoutMillis = 60_000)
        now = 0; assertEquals(SessionState.TimedOut, state())
    }

    @Test fun atomicRefreshRejectsChangedSession() = runTest {
        val stale = assertNotNull(session)
        session = stale.copy(userId = 2, createdAt = 50_000, lastActiveAt = 50_000)
        assertIs<Result.Error<AppError>>(manager.refreshSession(expectedSession = stale))
        assertEquals(50_000L, session?.lastActiveAt)
    }

}
