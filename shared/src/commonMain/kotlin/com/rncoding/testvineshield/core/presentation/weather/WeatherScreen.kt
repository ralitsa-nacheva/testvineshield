package com.rncoding.testvineshield.core.presentation.weather

import com.rncoding.testvineshield.core.presentation.components.InfoLabel
import com.rncoding.testvineshield.core.presentation.components.InfoHelp
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import com.rncoding.testvineshield.core.presentation.vineyard_details.localizedStage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
            if (state.loading || state.refreshing) item(key = "fixed:WeatherScreen:1", contentType = "fixed:WeatherScreen:1") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item(key = "fixed:WeatherScreen:2", contentType = "fixed:WeatherScreen:2") {
                Text(localizedWeatherError(error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = userAction(onRetry)) { Text(stringResource(Res.string.weather_retry)) }
            } }
            state.snapshot?.let { snapshot ->
                val zone = snapshot.vineyard.timeZone
                val display = presentWeather(snapshot, state.now)
                val coverage = previousDayCacheCoverage(snapshot, state.now)
                val stageEvidence = snapshot.stageEvidence?.takeIf { it.date == coverage?.date }?.result
                val historyRefresh=snapshot.cacheStatus.firstOrNull { it.kind=="history" }
                item(key = "fixed:WeatherScreen:3", contentType = "fixed:WeatherScreen:3") {
                    Text(snapshot.vineyard.name, style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(Res.string.weather_source))
                    Text(stringResource(Res.string.weather_zone, zone))
                    InfoHelp(title = stringResource(Res.string.info_weather_source), text = stringResource(Res.string.weather_location_cache) + "\n\n" + stringResource(Res.string.weather_offline_help))
                    Button(onClick = userAction(onRefresh), enabled = !state.refreshing, modifier = Modifier.fillMaxWidth()) { Text(if (state.refreshing) stringResource(Res.string.weather_refreshing) else stringResource(Res.string.weather_refresh)) }
                    InfoHelp(title = stringResource(Res.string.weather_refresh), text = stringResource(Res.string.weather_refresh_failure_help))
                }
                item(key = "fixed:WeatherScreen:4", contentType = "fixed:WeatherScreen:4") {
                    InfoLabel(title = stringResource(Res.string.weather_saved_refresh), text = stringResource(Res.string.weather_unfinished_help), style = MaterialTheme.typography.titleMedium)
                    if(snapshot.cacheStatus.isEmpty()) Text(stringResource(Res.string.weather_no_refresh))
                    snapshot.cacheStatus.forEach { status ->
                        val label=when(status.state) { "succeeded" -> stringResource(Res.string.weather_status_success); "failed" -> stringResource(Res.string.weather_status_failed); "started" -> stringResource(Res.string.weather_status_started); else -> stringResource(Res.string.weather_status_unknown) }
                        Text("${localizedRefreshKind(status.kind)}: $label")
                        Text(stringResource(Res.string.weather_saved_attempt, localizedWeatherTime(status.attemptedAt,zone), localizedWeatherTime(status.succeededAt,zone)))
                        status.errorCode?.let { Text(localizedSavedRefreshMessage(it)) }
                    }

                }
                state.refreshReport?.let { report -> item(key = "fixed:WeatherScreen:5", contentType = "fixed:WeatherScreen:5") {
                    Text(stringResource(Res.string.weather_attempt_completed, localizedWeatherTime(report.completedAt, zone)))
                    Text(report.historyError?.let { stringResource(Res.string.weather_history_failed, localizedWeatherError(it)) } ?: stringResource(Res.string.weather_history_completed))
                    Text(report.forecastError?.let { stringResource(Res.string.weather_forecast_failed, localizedWeatherError(it)) } ?: stringResource(Res.string.weather_forecast_completed))
                    InfoHelp(title = stringResource(Res.string.weather_refresh), text = stringResource(Res.string.weather_refresh_scope))
                } }
                state.refreshError?.let { error -> item(key = "fixed:WeatherScreen:6", contentType = "fixed:WeatherScreen:6") { Text(localizedWeatherError(error), color = MaterialTheme.colorScheme.error) } }
                item(key = "fixed:WeatherScreen:7", contentType = "fixed:WeatherScreen:7") {
                    InfoLabel(title = stringResource(Res.string.weather_history_heading), text = stringResource(Res.string.weather_history_coverage_help), style = MaterialTheme.typography.titleLarge)
                    InfoLabel(cacheAgeTitle(display.historyAge), cacheLabel(display.historyAge))
                    CoverageText(display.historyCoverage, zone)

                    Text(stringResource(Res.string.weather_last_history, localizedWeatherTime(historyRefresh?.succeededAt,zone)))
                    if (display.excludedHistoryHours > 0) Text(stringResource(Res.string.weather_excluded_hours, display.excludedHistoryHours))
                }
                display.history.lastOrNull()?.let { hour -> item(key = "fixed:WeatherScreen:8", contentType = "fixed:WeatherScreen:8") { WeatherHourCard(hour, zone, stringResource(Res.string.weather_latest_hour)) } }
                item(key = "fixed:WeatherScreen:9", contentType = "fixed:WeatherScreen:9") {
                    InfoLabel(title = stringResource(Res.string.weather_forecast_heading), text = stringResource(Res.string.weather_forecast_help), style = MaterialTheme.typography.titleLarge)
                    InfoLabel(cacheAgeTitle(display.forecastAge), cacheLabel(display.forecastAge))
                    Text(stringResource(Res.string.weather_refresh_start, localizedWeatherTime(display.forecastFetchedAt, zone)))
                    CoverageText(display.forecastCoverage, zone)
                    if (display.mixedForecastFetchTimes) Text(stringResource(Res.string.weather_mixed_fetch))

                }
                itemsIndexed(display.forecast.take(24), key = { index, cached -> "forecast:${cached.weather.timestamp}:${cached.fetchedAt}:$index" }, contentType = { _, _ -> "forecast-hour" }) { _, cached -> WeatherHourCard(cached.weather, zone, stringResource(Res.string.weather_forecast_hour)) }
                item(key = "fixed:WeatherScreen:10", contentType = "fixed:WeatherScreen:10") {
                    InfoLabel(title = stringResource(Res.string.monitor_weather_day), text = stringResource(Res.string.monitor_coverage_help), style = MaterialTheme.typography.titleMedium)
                    when {
                        coverage == null -> Text(stringResource(Res.string.monitor_zone_missing))
                        coverage.expectedHours == null -> Text(stringResource(Res.string.monitor_grid_mismatch, coverage.date.toString()))
                        else -> {
                            Text(stringResource(Res.string.monitor_day_coverage, coverage.date.toString(), coverage.matchedHours, coverage.expectedHours))
                            if (coverage.unexpectedRows > 0) {
                                Text(stringResource(Res.string.monitor_unexpected_rows, coverage.unexpectedRows))
                            }
                        }
                    }

                }
                item(key = "fixed:WeatherScreen:11", contentType = "fixed:WeatherScreen:11") {
                    Text(stringResource(Res.string.monitor_day_stages), style = MaterialTheme.typography.titleMedium)
                    when {
                        coverage == null -> Text(stringResource(Res.string.monitor_zone_missing))
                        stageEvidence is Result.Error -> {
                            Text(stringResource(Res.string.monitor_stages_failed), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = userAction(onRetry)) { Text(stringResource(Res.string.monitor_retry_stages)) }
                        }
                        stageEvidence == null -> Text(stringResource(Res.string.monitor_stages_loading))
                        snapshot.blocks.isEmpty() -> Text(stringResource(Res.string.monitor_no_blocks))
                        else -> Unit
                    }
                }
                if (stageEvidence is Result.Success) {
                    items(snapshot.blocks, key = { "day-stage-${it.blockId}" }) { block ->
                        val stage = stageEvidence.data[block.blockId]
                        Text("${block.name}: ${if (stage != null) localizedStage(stage) else stringResource(Res.string.monitor_no_day_observation)}")
                    }
                }
                item(key = "fixed:WeatherScreen:12", contentType = "fixed:WeatherScreen:12") {
                    InfoLabel(title = stringResource(Res.string.monitor_settings_heading), text = stringResource(Res.string.monitor_enabled_help), style = MaterialTheme.typography.titleLarge)
                    InfoHelp(title = stringResource(Res.string.info_risk), text = stringResource(Res.string.monitor_risk_help))
                    if (snapshot.monitoringError != null) {
                        Text(stringResource(Res.string.monitor_settings_failed, localizedWeatherError(snapshot.monitoringError)), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = userAction(onRetry)) { Text(stringResource(Res.string.monitor_retry_settings)) }
                    } else if (snapshot.configurations.isEmpty()) Text(stringResource(Res.string.monitor_no_settings))
                }
                if (snapshot.blocks.isNotEmpty() && snapshot.monitoringError == null) {
                    item(key = "botrytis-monitoring-draft", contentType = "botrytis-monitoring-draft") {
                        BotrytisDraftEditor(snapshot, state.savingMonitoringDraft, state.monitoringDraftNotice, onSaveDraft)
                    }
                }
                items(snapshot.configurations, key = { "monitoring:${it.vineyardId}:${it.blockId}:${it.diseaseId}:${it.modelType.name}" }, contentType = { "monitoring-card" }) { config ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(modelLabel(config.modelType), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(Res.string.monitor_block_value, snapshot.blocks.firstOrNull { it.blockId == config.blockId }?.name ?: stringResource(Res.string.weather_value_unavailable)))
                        Text(stringResource(Res.string.monitor_start_value, config.monitoringStartDate.toString()))
                        if (config.modelType == DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY) {
                            Text(stringResource(Res.string.monitor_soil_missing))
                        }
                        Text(if (config.enabled) stringResource(Res.string.monitor_saved_on) else stringResource(Res.string.monitor_saved_off))
                        val block = snapshot.blocks.firstOrNull { it.blockId == config.blockId }
                        Text(stringResource(Res.string.monitor_stage_value, localizedStage(block?.phenologicalStage)))
                        if (block?.phenologicalStage != null) {
                            Text(stringResource(Res.string.monitor_observed_date, block.stageObservedOn?.toString() ?: stringResource(Res.string.monitor_date_unknown)))
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
    notice: WeatherDraftNotice?,
    onSave: (Long, LocalDate) -> Unit
) {
    var selectedId by rememberSaveable(snapshot.vineyard.vineyardId) { mutableStateOf<Long?>(null) }
    var dateText by rememberSaveable(snapshot.vineyard.vineyardId) { mutableStateOf("") }
    val selected = snapshot.blocks.firstOrNull { it.blockId == selectedId }
    val date = try { LocalDate.parse(dateText.trim()) } catch (_: IllegalArgumentException) { null }
    val userActivity = LocalUserActivity.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoLabel(title = stringResource(Res.string.monitor_draft_heading), text = stringResource(Res.string.monitor_draft_help), style = MaterialTheme.typography.titleMedium)

        RecordChoice(
            label = stringResource(Res.string.monitor_block_label),
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
            nullLabel = stringResource(Res.string.monitor_choose_block)
        )
        OutlinedTextField(
            value = dateText,
            onValueChange = { userActivity(); dateText = it },
            label = { Text(stringResource(Res.string.monitor_date_label)) },
            enabled = !saving && selected != null,
            singleLine = true,
            isError = dateText.isNotBlank() && date == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = userAction { if (selected != null && date != null) onSave(selected.blockId, date) },
            enabled = !saving && selected != null && date != null
        ) { Text(if (saving) stringResource(Res.string.monitor_saving) else stringResource(Res.string.monitor_save_draft)) }
        if (notice != null) Text(localizedWeatherDraftNotice(notice))
    }
}

@Composable
private fun CoverageText(coverage: HourCoverage, zone: String) {
    Text(stringResource(Res.string.weather_coverage_counts, coverage.count, coverage.discontinuities))
    Text(stringResource(Res.string.weather_coverage_from, localizedWeatherTime(coverage.first, zone)))
    Text(stringResource(Res.string.weather_coverage_through, localizedWeatherTime(coverage.last, zone)))
}
@Composable
private fun WeatherHourCard(hour: WeatherHistoryDomainModel, zone: String, label: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(localizedWeatherTime(hour.timestamp, zone))
        Text(stringResource(Res.string.weather_temperature, localizedWeatherMetric(hour.hourlyTemp2m, "°C")))
        Text(stringResource(Res.string.weather_humidity, localizedWeatherMetric(hour.relativeHumidity2m, "%", 0.0..100.0)))
        Text(stringResource(Res.string.weather_precipitation, localizedWeatherMetric(hour.precipitation, "mm", 0.0..Double.MAX_VALUE)))
        Text(stringResource(Res.string.weather_wind, localizedWeatherMetric(hour.windSpeed10m, "km/h", 0.0..Double.MAX_VALUE)))
    } }
}
@Composable
private fun cacheLabel(age: CacheAge) = when (age) {
    CacheAge.EMPTY -> stringResource(Res.string.weather_cache_empty)
    CacheAge.RECENT -> stringResource(Res.string.weather_cache_recent)
    CacheAge.STALE -> stringResource(Res.string.weather_cache_stale)
    CacheAge.INVALID_CLOCK -> stringResource(Res.string.weather_cache_clock)
}
@Composable
private fun modelLabel(type: DiseaseRiskModelType) = when (type) {
    DiseaseRiskModelType.POWDERY_MILDEW_ASCOSPORE -> stringResource(Res.string.monitor_model_powdery_ascospore)
    DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS -> stringResource(Res.string.monitor_model_powdery_ucd)
    DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY -> stringResource(Res.string.monitor_model_downy_primary)
    DiseaseRiskModelType.DOWNY_MILDEW_SECONDARY -> stringResource(Res.string.monitor_model_downy_secondary)
    DiseaseRiskModelType.BOTRYTIS -> stringResource(Res.string.monitor_model_botrytis)
}

@Composable
private fun cacheAgeTitle(age: CacheAge): String = stringResource(when (age) {
    CacheAge.EMPTY -> Res.string.info_cache_empty
    CacheAge.RECENT -> Res.string.info_cache_recent
    CacheAge.STALE -> Res.string.info_cache_stale
    CacheAge.INVALID_CLOCK -> Res.string.info_cache_clock
})
