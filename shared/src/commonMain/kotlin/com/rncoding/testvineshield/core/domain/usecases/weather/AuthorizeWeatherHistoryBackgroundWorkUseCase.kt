
package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.auth.SessionState
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherLocation
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardWeatherLocationRepository
import com.rncoding.testvineshield.core.domain.security.SessionManager

sealed interface WeatherHistoryWorkAuthorization {
    data class Authorized(
        val location: VineyardWeatherLocation
    ) : WeatherHistoryWorkAuthorization

    data object NotAuthorized : WeatherHistoryWorkAuthorization

    data object VineyardNotFound : WeatherHistoryWorkAuthorization
}

class AuthorizeWeatherHistoryBackgroundWorkUseCase(
    private val sessionManager: SessionManager,
    private val locationRepository: VineyardWeatherLocationRepository
) {

    suspend operator fun invoke(
        scheduledUserId: Long,
        vineyardId: Long
    ): WeatherHistoryWorkAuthorization {

        require(scheduledUserId > 0L)
        require(vineyardId > 0L)

        // Check whether the persisted session is currently active.
        val state = when (val result = sessionManager.getSessionState()) {
            is Result.Success -> result.data
            is Result.Error -> throw IllegalStateException(
                "Unable to verify the active session: ${result.error}"
            )
        }

        if (state != SessionState.Active) {
            return WeatherHistoryWorkAuthorization.NotAuthorized
        }

        // Check that the active session belongs to the scheduled user.
        val session = when (val result = sessionManager.getSession()) {
            is Result.Success -> result.data
            is Result.Error -> throw IllegalStateException(
                "Unable to read the active session: ${result.error}"
            )
        }

        if (session?.userId != scheduledUserId) {
            return WeatherHistoryWorkAuthorization.NotAuthorized
        }

        // Verify ownership and obtain current coordinates.
        val location = locationRepository.getLocation(
            userId = scheduledUserId,
            vineyardId = vineyardId
        ) ?: return WeatherHistoryWorkAuthorization.VineyardNotFound

        return WeatherHistoryWorkAuthorization.Authorized(location)
    }
}
