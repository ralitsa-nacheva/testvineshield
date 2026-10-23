
package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.auth.SessionState
import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherLocation
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardWeatherLocationRepository
import com.rncoding.testvineshield.core.domain.security.SessionManager

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

import kotlinx.coroutines.runBlocking

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AuthorizeWeatherHistoryBackgroundWorkUseCaseTest {

    private val sessionManager =
        mockk<SessionManager>()

    private val locationRepository =
        mockk<VineyardWeatherLocationRepository>()

    private val useCase =
        AuthorizeWeatherHistoryBackgroundWorkUseCase(
            sessionManager = sessionManager,
            locationRepository = locationRepository
        )

    private val session = SessionDomainModel(
        userId = 1L,
        createdAt = 1_000L,
        lastActiveAt = 2_000L,
        expiresAt = 100_000L
    )

    private val location = VineyardWeatherLocation(
        vineyardId = 10L,
        latitude = 42.6977,
        longitude = 23.3219
    )

    private fun activeSession() {
        coEvery {
            sessionManager.getSessionState()
        } returns Result.Success(SessionState.Active)

        coEvery {
            sessionManager.getSession()
        } returns Result.Success(session)
    }

    @Test
    fun activeSessionAndOwnedVineyardAreAuthorized(): Unit = runBlocking {
        activeSession()

        coEvery {
            locationRepository.getLocation(
                userId = 1L,
                vineyardId = 10L
            )
        } returns location

        val result = useCase(
            scheduledUserId = 1L,
            vineyardId = 10L
        )

        val authorized =
            assertIs<WeatherHistoryWorkAuthorization.Authorized>(result)

        assertEquals(location, authorized.location)

        coVerify(exactly = 1) {
            locationRepository.getLocation(1L, 10L)
        }
    }

    @Test
    fun lockedSessionIsNotAuthorized(): Unit = runBlocking {
        coEvery {
            sessionManager.getSessionState()
        } returns Result.Success(SessionState.TimedOut)

        val result = useCase(1L, 10L)

        assertIs<WeatherHistoryWorkAuthorization.NotAuthorized>(result)

        coVerify(exactly = 0) {
            sessionManager.getSession()
        }

        coVerify(exactly = 0) {
            locationRepository.getLocation(any(), any())
        }
    }

    @Test
    fun loggedOutSessionIsNotAuthorized(): Unit = runBlocking {
        coEvery {
            sessionManager.getSessionState()
        } returns Result.Success(SessionState.LoggedOut)

        val result = useCase(1L, 10L)

        assertIs<WeatherHistoryWorkAuthorization.NotAuthorized>(result)

        coVerify(exactly = 0) {
            locationRepository.getLocation(any(), any())
        }
    }

    @Test
    fun differentSessionUserIsNotAuthorized(): Unit = runBlocking {
        coEvery {
            sessionManager.getSessionState()
        } returns Result.Success(SessionState.Active)

        coEvery {
            sessionManager.getSession()
        } returns Result.Success(
            session.copy(userId = 2L)
        )

        val result = useCase(
            scheduledUserId = 1L,
            vineyardId = 10L
        )

        assertIs<WeatherHistoryWorkAuthorization.NotAuthorized>(result)

        coVerify(exactly = 0) {
            locationRepository.getLocation(any(), any())
        }
    }

    @Test
    fun missingVineyardIsNotProcessed(): Unit = runBlocking {
        activeSession()

        coEvery {
            locationRepository.getLocation(1L, 10L)
        } returns null

        val result = useCase(1L, 10L)

        assertIs<WeatherHistoryWorkAuthorization.VineyardNotFound>(result)

        coVerify(exactly = 1) {
            locationRepository.getLocation(1L, 10L)
        }
    }

    @Test
    fun sessionStateReadFailureThrows(): Unit = runBlocking {
        coEvery {
            sessionManager.getSessionState()
        } returns Result.Error(AuthError.NoActiveSession)

        assertFailsWith<IllegalStateException> {
            useCase(1L, 10L)
        }

        coVerify(exactly = 0) {
            locationRepository.getLocation(any(), any())
        }
    }

    @Test
    fun sessionReadFailureThrows(): Unit = runBlocking {
        coEvery {
            sessionManager.getSessionState()
        } returns Result.Success(SessionState.Active)

        coEvery {
            sessionManager.getSession()
        } returns Result.Error(AuthError.NoActiveSession)

        assertFailsWith<IllegalStateException> {
            useCase(1L, 10L)
        }

        coVerify(exactly = 0) {
            locationRepository.getLocation(any(), any())
        }
    }

    @Test
    fun invalidUserIdIsRejected(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            useCase(
                scheduledUserId = 0L,
                vineyardId = 10L
            )
        }

        coVerify(exactly = 0) {
            sessionManager.getSessionState()
        }
    }

    @Test
    fun invalidVineyardIdIsRejected(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> {
            useCase(
                scheduledUserId = 1L,
                vineyardId = -1L
            )
        }

        coVerify(exactly = 0) {
            sessionManager.getSessionState()
        }
    }
}
