package com.rncoding.testvineshield.core.presentation.vineyard_details

import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertSeverity
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

data class VineyardDetailsUiModel(
    val vineyardId: Long,
    val name: String,
    val size: Double,
    val country: String,
    val city: String,
    val elevation: Int,
    val timeZone: String,
    val weather: WeatherUiModel?,
    val weatherRisk: WeatherRiskUiModel?,
    val blocks: List<BlockUiModel>,
    val activeDiseases: List<DiseaseUiModel>,
    val activeAlerts: List<AlertUiModel>
)

data class WeatherUiModel(
    val timestamp: Long,
    val temperature: Double,
    val relativeHumidity: Double,
    val precipitation: Double,
    val windSpeed: Double,
    val weatherCode: Int
)

data class WeatherRiskUiModel(
    val calculatedAt: LocalDate,
    val infectionScore: Int,
    val powderyMildewInitialInfection: Boolean,
    val wetnessHoursAt10C: Int,
    val hoursAt21C: Int,
    val daysFromBudBurst: Int
)

data class BlockUiModel(
    val blockId: Long,
    val name: String,
    val area: Int,
    val vineVariety: String,
    val phenologicalStage: PhenologicalStage?,
    val latestActivityType: String?,
    val latestActivityPriority: ActivityPriority?,
    val latestActivityStatus: ActivityStatus?,
    val activeDiseaseCount: Int,
    val activeAlertCount: Int
)

data class DiseaseUiModel(
    val occurrenceId: Long,
    val diseaseId: Long,
    val diseaseName: String,
    val blockId: Long?,
    val blockName: String?,
    val observedAt: LocalDate,
    val severity: Int,
    val status: DiseaseOccurrenceStatus
)

data class AlertUiModel(
    val alertId: Long,
    val diseaseId: Long,
    val diseaseName: String,
    val blockId: Long?,
    val blockName: String?,
    val createdAt: LocalDate,
    val alert: String,
    val status: AlertStatus,
    val severity: AlertSeverity,
    val phenologicalStage: PhenologicalStage
)