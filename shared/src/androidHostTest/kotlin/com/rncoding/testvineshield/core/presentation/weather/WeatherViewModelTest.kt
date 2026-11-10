package com.rncoding.testvineshield.core.presentation.weather

import androidx.lifecycle.ViewModelStore
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.weather.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    private val observe = mockk<ObserveOwnedWeatherUseCase>()
    private val refresh = mockk<RefreshOwnedWeatherUseCase>()
    private val clock = mockk<AppClock>()
    private val snapshot = mockk<VineyardWeatherSnapshot>()
    private lateinit var dispatcher: TestDispatcher
    private lateinit var store: ViewModelStore
    @Before fun before() {
        dispatcher = StandardTestDispatcher(); Dispatchers.setMain(dispatcher); store = ViewModelStore()
        every { clock.nowMillis() } returns 1_000_000L
        every { observe(10) } returns flowOf(Result.Success(snapshot))
        coEvery { refresh(10) } returns Result.Success(WeatherRefreshReport(null,null,1_000_000L))
    }
    @After fun after() {
        try { store.clear(); dispatcher.scheduler.runCurrent(); dispatcher.scheduler.advanceUntilIdle() }
        finally { Dispatchers.resetMain() }
    }
    private fun vm() = WeatherViewModel(10, observe, refresh, clock).also { store.put("weather", it) }
    @Test fun startDoesNotFetchAndTicksAgeWithoutUserInput() = runTest {
        val v = vm(); v.start(); v.start(); runCurrent()
        assertEquals(snapshot, v.state.value.snapshot); assertEquals(1_000_000L,v.state.value.now)
        every { clock.nowMillis() } returns 1_060_000L
        advanceTimeBy(60.seconds); runCurrent(); assertEquals(1_060_000L,v.state.value.now)
        coVerify(exactly = 0) { refresh(any()) }
        v.stop(); runCurrent(); assertNull(v.state.value.snapshot)
    }
    @Test fun duplicateRefreshIsBlockedAndReportDoesNotHideCachedSnapshot() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { refresh(10) } coAnswers { gate.await(); Result.Success(WeatherRefreshReport(NetworkError.Timeout,null,1_000_000L)) }
        val v = vm(); v.start(); runCurrent(); v.refresh(); v.refresh(); runCurrent()
        assertTrue(v.state.value.refreshing); assertEquals(snapshot, v.state.value.snapshot)
        gate.complete(Unit); runCurrent()
        coVerify(exactly = 1) { refresh(10) }
        assertFalse(v.state.value.refreshing); assertEquals(NetworkError.Timeout, v.state.value.refreshReport?.historyError)
        assertEquals(snapshot,v.state.value.snapshot); v.stop()
    }
    @Test fun ownershipFailureClearsDataAndCancelsRequest() = runTest {
        val data = MutableStateFlow<Result<VineyardWeatherSnapshot,AppError>>(Result.Success(snapshot))
        every { observe(10) } returns data
        var cleaned = false
        coEvery { refresh(10) } coAnswers { try { awaitCancellation() } finally { cleaned = true } }
        val v = vm(); v.start(); runCurrent(); v.refresh(); runCurrent()
        data.value = Result.Error(AuthError.NoActiveSession); runCurrent()
        assertTrue(cleaned); assertNull(v.state.value.snapshot); assertFalse(v.state.value.refreshing)
        assertEquals(AuthError.NoActiveSession.userMessage, v.state.value.error); v.stop()
    }
    @Test fun lifecycleStopCancelsWorkAndRestartReloadsWithoutOldReport() = runTest {
        var cleaned = false
        coEvery { refresh(10) } coAnswers { try { awaitCancellation() } finally { cleaned = true } }
        val v = vm(); v.start(); runCurrent(); v.refresh(); runCurrent(); v.stop(); runCurrent()
        assertTrue(cleaned); assertNull(v.state.value.snapshot); assertNull(v.state.value.refreshReport)
        v.start(); runCurrent(); assertEquals(snapshot,v.state.value.snapshot); assertFalse(v.state.value.refreshing)
        v.stop()
    }
    @Test fun failedReadCanRetryAndRefreshFailureAllowsAnotherAttempt() = runTest {
        every { observe(10) } returns flowOf(Result.Error(UnexpectedError()))
        val v = vm(); v.start(); runCurrent(); assertNotNull(v.state.value.error)
        every { observe(10) } returns flowOf(Result.Success(snapshot))
        v.retry(); runCurrent(); assertNull(v.state.value.error)
        coEvery { refresh(10) } returns Result.Error(NetworkError.Timeout)
        v.refresh(); runCurrent(); assertNotNull(v.state.value.refreshError); assertEquals(snapshot,v.state.value.snapshot)
        coEvery { refresh(10) } returns Result.Success(WeatherRefreshReport(null,null,1_000_000L))
        v.refresh(); runCurrent(); assertNull(v.state.value.refreshError); assertNotNull(v.state.value.refreshReport)
        v.stop()
    }
}
