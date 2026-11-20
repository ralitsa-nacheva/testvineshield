package com.rncoding.testvineshield.core.presentation.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.weather.*
import kotlinx.coroutines.*
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.*

data class WeatherScreenState(
    val snapshot: VineyardWeatherSnapshot? = null,
    val loading: Boolean = true,
    val now: Long = 0,
    val error: AppError? = null,
    val refreshing: Boolean = false,
    val refreshReport: WeatherRefreshReport? = null,
    val refreshError: AppError? = null,
    val savingMonitoringDraft: Boolean = false,
    val monitoringDraftNotice: WeatherDraftNotice? = null
)

class WeatherViewModel(
    private val vineyardId: Long,
    private val observe: ObserveOwnedWeatherUseCase,
    private val refresh: RefreshOwnedWeatherUseCase,
    private val clock: AppClock,
    private val saveDraft: SaveBotrytisMonitoringDraftUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(WeatherScreenState())
    val state = _state.asStateFlow()
    private var observation: Job? = null
    private var refreshJob: Job? = null
    private var draftJob: Job? = null
    private var generation = 0

    /** Lifecycle STARTED only. Ticks update age labels, never count as user activity. */
    fun start() {
        if (observation?.isActive == true) return
        val token = ++generation
        _state.value = WeatherScreenState(now = clock.nowMillis())
        observation = viewModelScope.launch {
            combine(observe(vineyardId), flow {
                while (true) { emit(clock.nowMillis()); delay(60.seconds) }
            }) { result, now -> result to now }.collect { (result, now) ->
                if (token != generation) return@collect
                when (result) {
                    is Result.Success -> _state.update { it.copy(snapshot = result.data, now = now, loading = false, error = null) }
                    is Result.Error -> {
                        refreshJob?.cancel()
                        draftJob?.cancel()
                        _state.value = WeatherScreenState(now = now, loading = false, error = result.error)
                    }
                }
            }
        }
    }
    fun stop() {
        ++generation
        observation?.cancel(); observation = null
        refreshJob?.cancel(); refreshJob = null
        draftJob?.cancel(); draftJob = null
        _state.value = WeatherScreenState()
    }
    fun retry() { stop(); start() }
    fun saveMonitoringDraft(blockId: Long, startDate: LocalDate) {
        val current = _state.value
        if (current.savingMonitoringDraft || observation?.isActive != true ||
            current.snapshot?.blocks?.any { it.blockId == blockId } != true
        ) return

        val token = generation
        _state.update { it.copy(savingMonitoringDraft = true, monitoringDraftNotice = null) }
        draftJob = viewModelScope.launch {
            try {
                val notice = when (val result = saveDraft(vineyardId, blockId, startDate)) {
                    is Result.Error -> WeatherDraftNotice.Failure(result.error)
                    is Result.Success -> WeatherDraftNotice.Outcome(result.data)
                }
                currentCoroutineContext().ensureActive()
                if (token == generation && _state.value.snapshot != null) {
                    _state.update { it.copy(monitoringDraftNotice = notice) }
                }
            } finally {
                if (token == generation) _state.update { it.copy(savingMonitoringDraft = false) }
            }
        }
    }

    fun refresh() {
        if (_state.value.snapshot == null || _state.value.refreshing || observation?.isActive != true) return
        val token = generation
        _state.update { it.copy(refreshing = true, refreshReport = null, refreshError = null) }
        refreshJob = viewModelScope.launch {
            try {
                val result = refresh(vineyardId)
                currentCoroutineContext().ensureActive()
                if (token == generation && _state.value.snapshot != null) when (result) {
                    is Result.Success -> _state.update { it.copy(refreshReport = result.data, now = clock.nowMillis()) }
                    is Result.Error -> _state.update { it.copy(refreshError = result.error) }
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (token == generation && _state.value.snapshot != null) _state.update { it.copy(refreshError = UnexpectedError()) }
            } finally {
                if (token == generation) _state.update { it.copy(refreshing = false) }
            }
        }
    }
}
