package com.rncoding.testvineshield.core.presentation.weather

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    WeatherScreen(state, onBack, vm::retry, vm::refresh)
}

@Composable
fun WeatherScreen(state: WeatherScreenState, onBack: () -> Unit, onRetry: () -> Unit, onRefresh: () -> Unit) {
    Scaffold(topBar = { RecordTopBar("Weather and monitoring", onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().widthIn(max = 840.dp), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.refreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = userAction(onRetry)) { Text("Retry loading") }
            } }
            state.snapshot?.let { snapshot ->
                val zone = snapshot.vineyard.timeZone
                val display = presentWeather(snapshot, state.now)
                val coverage = previousDayCacheCoverage(snapshot, state.now)
                val stageEvidence = snapshot.stageEvidence?.takeIf { it.date == coverage?.date }?.result
                val historyRefresh=snapshot.cacheStatus.firstOrNull { it.kind=="history" }
                item {
                    Text(snapshot.vineyard.name, style = MaterialTheme.typography.headlineSmall)
                    Text("Source: Open-Meteo · modeled weather, not a vineyard sensor measurement.")
                    Text("Vineyard time zone: $zone")
                    Text("Only weather attributed to this saved location is shown. Older unattributed/previous-location cache rows are retained but excluded; refresh to obtain current-location data.")
                    Text("Cached records are available offline. A refresh needs internet and sends the vineyard coordinates and time zone to Open-Meteo.")
                    Button(onClick = userAction(onRefresh), enabled = !state.refreshing, modifier = Modifier.fillMaxWidth()) { Text(if (state.refreshing) "Refreshing…" else "Refresh history and forecast") }
                    Text("A failed refresh alone does not establish whether the device is offline. Previously saved data remains visible.")
                }
                item {
                    Text("Saved refresh status",style=MaterialTheme.typography.titleMedium)
                    if(snapshot.cacheStatus.isEmpty()) Text("No completed refresh is recorded for this location yet.")
                    snapshot.cacheStatus.forEach { status ->
                        val label=when(status.state) { "succeeded" -> "completed"; "failed" -> "failed or interrupted"; "started" -> "started; completion not recorded"; else -> "status unavailable" }
                        Text("${status.kind}: $label")
                        Text("Attempt: ${weatherTime(status.attemptedAt,zone)} · Last success: ${weatherTime(status.succeededAt,zone)}")
                        status.errorCode?.let { Text(savedRefreshMessage(it)) }
                    }
                    Text("A saved unfinished attempt is not proof that a request is still running. Coverage is cached evidence, not a disease safety assessment.")
                }
                state.refreshReport?.let { report -> item {
                    Text("Refresh attempt completed: ${weatherTime(report.completedAt, zone)}")
                    Text(report.historyError?.let { "History failed: ${it.userMessage}" } ?: "History request completed.")
                    Text(report.forecastError?.let { "Forecast failed: ${it.userMessage}" } ?: "Forecast request completed.")
                    Text("This refresh updates weather caches only; it does not run disease models or create alerts.")
                } }
                state.refreshError?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
                item {
                    Text("Recent modeled history", style = MaterialTheme.typography.titleLarge)
                    Text(cacheLabel(display.historyAge))
                    CoverageText(display.historyCoverage, zone)
                    Text("Coverage is the stored part of the last seven elapsed days, not proof of a complete disease-model input window.")
                    Text("Last completed history refresh: ${weatherTime(historyRefresh?.succeededAt,zone)}")
                    if (display.excludedHistoryHours > 0) Text("${display.excludedHistoryHours} incomplete/future history hours excluded; check the device clock.")
                }
                display.history.lastOrNull()?.let { hour -> item { WeatherHourCard(hour, zone, "Latest cached time step (at least one hour old)") } }
                item {
                    Text("Forecast", style = MaterialTheme.typography.titleLarge)
                    Text(cacheLabel(display.forecastAge))
                    Text("Recorded refresh start: ${weatherTime(display.forecastFetchedAt, zone)}")
                    CoverageText(display.forecastCoverage, zone)
                    if (display.mixedForecastFetchTimes) Text("Stored forecast contains inconsistent fetch times. Refresh before relying on it.")
                    Text("Showing up to the next 24 cached hourly values. Forecasts are predictions; refresh does not guarantee local conditions.")
                }
                items(display.forecast.take(24)) { cached -> WeatherHourCard(cached.weather, zone, "Forecast hour") }
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
                    Text("Read-only in this checkpoint. Enabled means a saved preference, not that a validated model has run or that the vineyard is safe.")
                    Text("Disease risk is not assessed by this screen. No configurations or alerts must never be interpreted as no disease risk.")
                    if (snapshot.monitoringError != null) {
                        Text("Monitoring settings could not be read: ${snapshot.monitoringError.userMessage}", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = userAction(onRetry)) { Text("Retry loading settings") }
                    } else if (snapshot.configurations.isEmpty()) Text("No monitoring configurations saved.")
                }
                items(snapshot.configurations) { config ->
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(modelLabel(config.modelType), style = MaterialTheme.typography.titleMedium)
                        Text("Block: ${snapshot.blocks.firstOrNull { it.blockId == config.blockId }?.name ?: "Unavailable"}")
                        Text("Monitoring start: ${config.monitoringStartDate}")
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
private fun cacheLabel(age: CacheAge) = when (age) {
    CacheAge.EMPTY -> "No cached data available."
    CacheAge.RECENT -> "Cache age is within the display threshold; this is not a quality or disease-risk guarantee."
    CacheAge.STALE -> "Cache is stale or its forecast horizon has ended. Refresh when connected."
    CacheAge.INVALID_CLOCK -> "Cache timestamps are inconsistent with the device clock or fetch metadata."
}
private fun modelLabel(type: DiseaseRiskModelType) = when (type) {
    DiseaseRiskModelType.POWDERY_MILDEW_ASCOSPORE -> "Powdery mildew · ascospore model"
    DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS -> "Powdery mildew · UC Davis model"
    DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY -> "Downy mildew · primary model"
    DiseaseRiskModelType.DOWNY_MILDEW_SECONDARY -> "Downy mildew · secondary model"
    DiseaseRiskModelType.BOTRYTIS -> "Botrytis model"
}
