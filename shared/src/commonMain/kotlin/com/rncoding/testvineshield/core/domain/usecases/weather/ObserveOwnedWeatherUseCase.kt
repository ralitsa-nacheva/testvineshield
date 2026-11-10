package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.disease_risk.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repositories.WeatherForecastRepository
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class VineyardWeatherSnapshot(
    val vineyard: VineyardDomainModel,
    val history: List<WeatherHistoryDomainModel>,
    val forecast: List<CachedForecastHour>,
    val configurations: List<DiseaseRiskMonitoringConfiguration>,
    val blocks: List<BlockDomainModel>,
    val monitoringError: AppError? = null
)

/** Subscribe to child caches only after resolving the current owner's vineyard. */
class ObserveOwnedWeatherUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val history: WeatherHistoryRepository,
    private val forecast: WeatherForecastRepository,
    private val configurations: DiseaseRiskMonitoringConfigurationRepository,
    private val blocks: BlockRepository,
    private val clock: AppClock
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(vineyardId: Long): Flow<Result<VineyardWeatherSnapshot, AppError>> {
        if (vineyardId <= 0) return flowOf(Result.Error(WeatherHistoryError.InvalidInput("Invalid vineyard ID")))
        return users.observeUser().flatMapLatest { user ->
            if (user == null) flowOf<Result<VineyardWeatherSnapshot, AppError>>(Result.Error(AuthError.NoActiveSession))
            else vineyards.observeVineyard(user.userId, vineyardId).flatMapLatest { parent ->
                if (parent == null) flowOf<Result<VineyardWeatherSnapshot, AppError>>(Result.Error(DatabaseError.NotFound("Vineyard")))
                else combine(
                    history.observeHistoricalVineyardWeather(vineyardId, clock.nowMillis() - HISTORY_WINDOW_MILLIS),
                    forecast.observeCachedForecast(vineyardId),
                    configurations.observeConfigurationsForVineyard(vineyardId)
                        .map<List<DiseaseRiskMonitoringConfiguration>, Result<List<DiseaseRiskMonitoringConfiguration>, AppError>> { Result.Success(it) }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            emit(Result.Error(UnexpectedError()))
                        },
                    blocks.observeBlocks(vineyardId)
                ) { past, future, configs, children ->
                    // A repository contract violation must never expose another vineyard's rows.
                    check(past.all { it.vineyardId == vineyardId } && future.all { it.weather.vineyardId == vineyardId })
                    val savedConfigs = when (configs) {
                        is Result.Success -> configs.data
                        is Result.Error -> emptyList()
                    }
                    val monitoringError = when (configs) {
                        is Result.Success -> null
                        is Result.Error -> configs.error
                    }
                    check(savedConfigs.all { it.vineyardId == vineyardId } && children.all { it.vineyardId == vineyardId })
                    val snapshot: Result<VineyardWeatherSnapshot, AppError> =
                        Result.Success(VineyardWeatherSnapshot(parent, past, future, savedConfigs, children, monitoringError))
                    snapshot
                }
            }
        }.buffer(0).catch { error ->
            if (error is CancellationException) throw error
            emit(Result.Error(UnexpectedError()))
        }
    }
    companion object { const val HISTORY_WINDOW_MILLIS = 7L * 24 * 3_600_000 }
}
