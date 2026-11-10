package com.rncoding.testvineshield.core.domain.usecases.weather

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repositories.WeatherForecastRepository
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.time.AppClock
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Test
import kotlin.test.*
class TypedForecastRefreshTest {
    @Test fun forecastErrorReachesReportWithoutLosingHistoryOutcome() = runTest {
        val users=mockk<AuthenticatedUserProvider>(); val vineyards=mockk<VineyardRepository>(); val history=mockk<WeatherHistoryRepository>()
        val forecast=mockk<WeatherForecastRepository>(); val clock=mockk<AppClock>()
        val date=LocalDate(2026,11,10)
        val parent=VineyardDomainModel(10,1,"North",1.0,"BG","Plovdiv",42.7,23.3,"Europe/Sofia",null,0,date,date)
        every { users.currentUserOrNull() } returns UserDomainModel(1,"a@example.com")
        every { clock.nowMillis() } returns 1_800_000_000_000L
        coEvery { vineyards.getVineyard(1,10) } returns Result.Success(parent)
        coEvery { history.refreshWeatherHistory(10,42.7,23.3) } returns Result.Success(WeatherHistoryRefreshResult(10,1_800_000_000_000L,24,0))
        coEvery { forecast.refreshForecast(10,42.7,23.3,any()) } returns Result.Error(NetworkError.Timeout)
        val report=assertIs<Result.Success<WeatherRefreshReport>>(RefreshOwnedWeatherUseCase(users,vineyards,history,forecast,clock)(10)).data
        assertNull(report.historyError); assertEquals(NetworkError.Timeout,report.forecastError)
    }
}
