package com.rncoding.testvineshield.core.presentation.weather

import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.rncoding.testvineshield.core.domain.usecases.weather.VineyardWeatherSnapshot
import com.rncoding.testvineshield.core.presentation.components.RecordChoice
import com.rncoding.testvineshield.core.presentation.root.LocalUserActivity
import kotlinx.datetime.LocalDate
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.presentation.components.RecordTopBar
import com.rncoding.testvineshield.core.presentation.root.userAction
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun WeatherScreenRoot(ownerId: Long, vineyardId: Long, onBack: () -> Unit) {
    val vm = koinViewModel<WeatherViewModel>(key = "weather-$ownerId-$vineyardId", parameters = { parametersOf(vineyardId) })
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleStartEffect(vm) { vm.start(); onStopOrDispose { vm.stop() } }
    WeatherScreen(state, onBack, vm::retry, vm::refresh, vm::saveMonitoringDraft)
}

@Composable
fun WeatherScreen(
    state: WeatherScreenState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onSaveDraft: (Long, LocalDate) -> Unit
) {
    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.weather_heading), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().widthIn(max = 840.dp), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.refreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = userAction(onRetry)) { Text(stringResource(Res.string.weather_retry)) }
            } }
            state.snapshot?.let { snapshot ->
                val zone = snapshot.vineyard.timeZone
                val display = presentWeather(snapshot, state.now)
                val coverage = previousDayCacheCoverage(snapshot, state.now)
                val stageEvidence = snapshot.stageEvidence?.takeIf { it.date == coverage?.date }?.result
                val historyRefresh=snapshot.cacheStatus.firstOrNull { it.kind=="history" }
                item {
                    Text(snapshot.vineyard.name, style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(Res.string.weather_source))
                    Text(stringResource(Res.string.weather_zone, zone))
                    Text(stringResource(Res.string.weather_location_cache))
                    Text(stringResource(Res.string.weather_offline_help))
                    Button(onClick = userAction(onRefresh), enabled = !state.refreshing, modifier = Modifier.fillMaxWidth()) { Text(if (state.refreshing) stringResource(Res.string.weather_refreshing) else stringResource(Res.string.weather_refresh)) }
                    Text(stringResource(Res.string.weather_refresh_failure_help))
                }
                item {
                    Text(stringResource(Res.string.weather_saved_refresh),style=MaterialTheme.typography.titleMedium)
                    if(snapshot.cacheStatus.isEmpty()) Text(stringResource(Res.string.weather_no_refresh))
                    snapshot.cacheStatus.forEach { status ->
                        val label=when(status.state) { "succeeded" -> stringResource(Res.string.weather_status_success); "failed" -> stringResource(Res.string.weather_status_failed); "started" -> stringResource(Res.string.weather_status_started); else -> stringResource(Res.string.weather_status_unknown) }
                        Text("${status.kind}: $label")
                        Text(stringResource(Res.string.weather_saved_attempt, weatherTime(status.attemptedAt,zone), weatherTime(status.succeededAt,zone)))
                        status.errorCode?.let { Text(savedRefreshMessage(it)) }
                    }
                    Text(stringResource(Res.string.weather_unfinished_help))
                }
                state.refreshReport?.let { report -> item {
                    Text("Refresh attempt completed: ${weatherTime(report.completedAt, zone)}")
                    Text(report.historyError?.let { "History failed: ${it.userMessage}" } ?: "History request completed.")
                    Text(report.forecastError?.let { "Forecast failed: ${it.userMessage}" } ?: "Forecast request completed.")
                    Text("This refresh updates weather caches only; it does not run disease models or create alerts.")
                } }
                state.refreshError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
                item {
                    Text(stringResource(Res.string.weather_history_heading), style = MaterialTheme.typography.titleLarge)
                    Text(cacheLabel(display.historyAge))
                    CoverageText(display.historyCoverage, zone)
                    Text(stringResource(Res.string.weather_history_coverage_help))
                    Text(stringResource(Res.string.weather_last_history, weatherTime(historyRefresh?.succeededAt,zone)))
                    if (display.excludedHistoryHours > 0) Text(stringResource(Res.string.weather_excluded_hours, display.excludedHistoryHours))
                }
                display.history.lastOrNull()?.let { hour -> item { WeatherHourCard(hour, zone, stringResource(Res.string.weather_latest_hour)) } }
                item {
                    Text(stringResource(Res.string.weather_forecast_heading), style = MaterialTheme.typography.titleLarge)
                    Text(cacheLabel(display.forecastAge))
                    Text(stringResource(Res.string.weather_refresh_start, weatherTime(display.forecastFetchedAt, zone)))
                    CoverageText(display.forecastCoverage, zone)
                    if (display.mixedForecastFetchTimes) Text(stringResource(Res.string.weather_mixed_fetch))
                    Text(stringResource(Res.string.weather_forecast_help))
                }
                items(display.forecast.take(24)) { cached -> WeatherHourCard(cached.weather, zone, stringResource(Res.string.weather_forecast_hour)) }
                item {
                    Text("Botrytis weather input · previous local day", style = MaterialTheme.typography.titleMedium)
                    when {
                        coverage == null -> Text("The vineyard time zone is unavailable.")
                        coverage.expectedHours == null -> Text("${coverage.date}: This day does not fit the hourly weather grid.")
                        else -> {
                            Text("${coverage.date}: ${coverage.matchedHours} of ${coverage.expectedHours} expected weather hours cached.")
                            if (coverage.unexpectedRows > 0) {
                                Text("The cache also contains ${coverage.unexpectedRows} duplicate or off-grid rows for this day.")
                            }
                        }
                    }
                    Text("Weather coverage counts timestamps. Wetness and model validation require separate checks.")
                }
                item {
                    Text("Block stages for the previous local day", style = MaterialTheme.typography.titleMedium)
                    when {
                        coverage == null -> Text("The vineyard time zone is unavailable.")
                        stageEvidence is Result.Error -> {
                            Text("Stage observations could not be read.", color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = userAction(onRetry)) { Text("Retry loading stages") }
                        }
                        stageEvidence == null -> Text("Loading stage observations…")
                        snapshot.blocks.isEmpty() -> Text("No blocks saved.")
                        else -> Unit
                    }
                }
                if (stageEvidence is Result.Success) {
                    items(snapshot.blocks, key = { "day-stage-${it.blockId}" }) { block ->
                        val stage = stageEvidence.data[block.blockId]
                        Text("${block.name}: ${stage?.dbValue?.replace('_', ' ') ?: "No observation for this date"}")
                    }
                }
                item {
                    Text("Saved monitoring configuration", style = MaterialTheme.typography.titleLarge)
                    Text("An enabled setting does not confirm that a validated model has run.")
                    Text("Disease risk is not assessed by this screen. No configurations or alerts must never be interpreted as no disease risk.")
                    if (snapshot.monitoringError != null) {
                        Text("Monitoring settings could not be read: ${snapshot.monitoringError.userMessage}", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = userAction(onRetry)) { Text("Retry loading settings") }
                    } else if (snapshot.configurations.isEmpty()) Text("No monitoring configurations saved.")
                }
                if (snapshot.blocks.isNotEmpty() && snapshot.monitoringError == null) {
                    item(key = "botrytis-monitoring-draft") {
                        BotrytisDraftEditor(snapshot, state.savingMonitoringDraft, state.monitoringDraftMessage, onSaveDraft)
                    }
                }
                items(snapshot.configurations) { config ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(modelLabel(config.modelType), style = MaterialTheme.typography.titleMedium)
                        Text("Block: ${snapshot.blocks.firstOrNull { it.blockId == config.blockId }?.name ?: "Unavailable"}")
                        Text("Monitoring start: ${config.monitoringStartDate}")
                        if (config.modelType == DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY) {
                            Text("Evaluation unavailable: this app lacks the required soil-wetness input.")
                        }
                        Text(if (config.enabled) "Saved setting: enabled" else "Saved setting: disabled")
                        val block = snapshot.blocks.firstOrNull { it.blockId == config.blockId }
                        Text("Recorded block stage: ${block?.phenologicalStage?.dbValue?.replace('_', ' ') ?: "Unknown"}")
                        if (block?.phenologicalStage != null) {
                            Text("Observed on: ${block.stageObservedOn ?: "Date unknown"}")
                        }
                    } }
                }
            }
        }
    }
}
@Composable
private fun BotrytisDraftEditor(
    snapshot: VineyardWeatherSnapshot,
    saving: Boolean,
    message: String?,
    onSave: (Long, LocalDate) -> Unit
) {
    var selectedId by rememberSaveable(snapshot.vineyard.vineyardId) { mutableStateOf<Long?>(null) }
    var dateText by rememberSaveable(snapshot.vineyard.vineyardId) { mutableStateOf("") }
    val selected = snapshot.blocks.firstOrNull { it.blockId == selectedId }
    val date = try { LocalDate.parse(dateText.trim()) } catch (_: IllegalArgumentException) { null }
    val userActivity = LocalUserActivity.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Botrytis monitoring draft", style = MaterialTheme.typography.titleMedium)
        Text("Save a start date for a block. Monitoring stays off.")
        RecordChoice(
            label = "Block",
            selected = selected,
            values = snapshot.blocks,
            enabled = !saving,
            text = { it.name },
            changed = { block ->
                selectedId = block?.blockId
                dateText = snapshot.configurations.firstOrNull {
                    it.blockId == block?.blockId && it.modelType == DiseaseRiskModelType.BOTRYTIS && !it.enabled
                }?.monitoringStartDate?.toString().orEmpty()
            },
            allowUnknown = false,
            nullLabel = "Choose a block"
        )
        OutlinedTextField(
            value = dateText,
            onValueChange = { userActivity(); dateText = it },
            label = { Text("Start date (YYYY-MM-DD)") },
            enabled = !saving && selected != null,
            singleLine = true,
            isError = dateText.isNotBlank() && date == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = userAction { if (selected != null && date != null) onSave(selected.blockId, date) },
            enabled = !saving && selected != null && date != null
        ) { Text(if (saving) "Saving…" else "Save draft") }
        if (message != null) Text(message)
    }
}

@Composable
private fun CoverageText(coverage: HourCoverage, zone: String) {
    Text("${coverage.count} cached hours · ${coverage.discontinuities} discontinuities between stored timestamps")
    Text("From: ${weatherTime(coverage.first, zone)}")
    Text("Through: ${weatherTime(coverage.last, zone)}")
}
@Composable
private fun WeatherHourCard(hour: WeatherHistoryDomainModel, zone: String, label: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(weatherTime(hour.timestamp, zone))
        Text("Temperature: ${weatherMetric(hour.hourlyTemp2m, "°C")}")
        Text("Relative humidity: ${weatherMetric(hour.relativeHumidity2m, "%", 0.0..100.0)}")
        Text("Precipitation (preceding hour): ${weatherMetric(hour.precipitation, "mm", 0.0..Double.MAX_VALUE)}")
        Text("Wind at 10 m: ${weatherMetric(hour.windSpeed10m, "km/h", 0.0..Double.MAX_VALUE)}")
    } }
}
@Composable
private fun cacheLabel(age: CacheAge) = when (age) {
    CacheAge.EMPTY -> stringResource(Res.string.weather_cache_empty)
    CacheAge.RECENT -> stringResource(Res.string.weather_cache_recent)
    CacheAge.STALE -> stringResource(Res.string.weather_cache_stale)
    CacheAge.INVALID_CLOCK -> stringResource(Res.string.weather_cache_clock)
}
private fun modelLabel(type: DiseaseRiskModelType) = when (type) {
    DiseaseRiskModelType.POWDERY_MILDEW_ASCOSPORE -> "Powdery mildew · ascospore model"
    DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS -> "Powdery mildew · UC Davis model"
    DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY -> "Downy mildew · primary model"
    DiseaseRiskModelType.DOWNY_MILDEW_SECONDARY -> "Downy mildew · secondary model"
    DiseaseRiskModelType.BOTRYTIS -> "Botrytis model"
}
