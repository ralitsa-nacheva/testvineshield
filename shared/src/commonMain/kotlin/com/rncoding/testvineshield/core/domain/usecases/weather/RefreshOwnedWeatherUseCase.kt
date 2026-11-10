package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repositories.WeatherForecastRepository
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.datetime.TimeZone

data class WeatherRefreshReport(val historyError: AppError?, val forecastError: AppError?, val completedAt: Long)

/** Explicit cache refresh. Does not run a risk model or create alerts. */
class RefreshOwnedWeatherUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val history: WeatherHistoryRepository,
    private val forecast: WeatherForecastRepository,
    private val clock: AppClock
) {
    suspend operator fun invoke(vineyardId: Long): Result<WeatherRefreshReport, AppError> {
        if (vineyardId <= 0) return Result.Error(WeatherHistoryError.InvalidInput("Invalid vineyard ID"))
        val owner = users.currentUserOrNull()?.userId ?: return Result.Error(AuthError.NoActiveSession)
        return try {
            val parent = when (val result = vineyards.getVineyard(owner, vineyardId)) {
                is Result.Error -> return result
                is Result.Success -> result.data
            }
            if (!validLocation(parent)) return Result.Error(WeatherHistoryError.InvalidInput("Invalid coordinates or time zone"))
            if (!sameOwner(owner)) return Result.Error(AuthError.NoActiveSession)
            val historyError = try {
                when (val result = history.refreshWeatherHistory(vineyardId, parent.latitude, parent.longitude)) {
                    is Result.Success -> null
                    is Result.Error -> result.error
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { UnexpectedError() }
            currentCoroutineContext().ensureActive()
            if (!sameOwner(owner)) return Result.Error(AuthError.NoActiveSession)
            // Do not start a second request with a location edited during the first one.
            val latest = when (val result = vineyards.getVineyard(owner, vineyardId)) {
                is Result.Error -> return result
                is Result.Success -> result.data
            }
            if (latest.latitude != parent.latitude || latest.longitude != parent.longitude || latest.timeZone != parent.timeZone) {
                return Result.Error(WeatherHistoryError.InvalidInput("Vineyard location changed; refresh again"))
            }
            if (!sameOwner(owner)) return Result.Error(AuthError.NoActiveSession)
            val forecastError = try {
                when (val result = forecast.refreshForecast(vineyardId, parent.latitude, parent.longitude, clock.nowMillis())) {
                    is Result.Success -> null
                    is Result.Error -> result.error
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { UnexpectedError() }
            currentCoroutineContext().ensureActive()
            if (!sameOwner(owner)) Result.Error(AuthError.NoActiveSession)
            else Result.Success(WeatherRefreshReport(historyError, forecastError, clock.nowMillis()))
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { Result.Error(UnexpectedError()) }
    }
    private fun sameOwner(owner: Long) = users.currentUserOrNull()?.userId == owner
    private fun validLocation(parent: VineyardDomainModel): Boolean =
        parent.latitude.isFinite() && parent.latitude in -90.0..90.0 &&
            parent.longitude.isFinite() && parent.longitude in -180.0..180.0 &&
            runCatching { TimeZone.of(parent.timeZone) }.isSuccess
}
