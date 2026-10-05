package com.rncoding.testvineshield.core.presentation.vineyard_details

import com.rncoding.testvineshield.core.domain.datamodels.VineyardDetailedSummary

class VineyardDetailsUiMapper {

    fun toUi(
        summary: VineyardDetailedSummary
    ): VineyardDetailsUiModel {
        return VineyardDetailsUiModel(
            vineyardId = summary.vineyard.vineyardId,
            name = summary.vineyard.name,
            size = summary.vineyard.size,
            country = summary.vineyard.country,
            city = summary.vineyard.city,
            elevation = summary.vineyard.elevation,
            timeZone = summary.vineyard.timeZone,

            weather = summary.latestWeather?.let { weather ->
                WeatherUiModel(
                    timestamp = weather.timestamp,
                    temperature = weather.temperature,
                    relativeHumidity = weather.relativeHumidity,
                    precipitation = weather.precipitation,
                    windSpeed = weather.windSpeed,
                    weatherCode = weather.weatherCode
                )
            },

            weatherRisk =
                summary.latestWeatherRisk?.let { risk ->
                    WeatherRiskUiModel(
                        calculatedAt = risk.calculatedAt,
                        infectionScore = risk.infectionScore,
                        powderyMildewInitialInfection =
                            risk.powderyMildewInitialInfection,
                        wetnessHoursAt10C =
                            risk.wetnessHoursAt10C,
                        hoursAt21C =
                            risk.hoursAt21C,
                        daysFromBudBurst =
                            risk.daysFromBudBurst
                    )
                },

            blocks = summary.blocks.map { block ->
                BlockUiModel(
                    blockId = block.blockId,
                    name = block.name,
                    area = block.area,
                    vineVariety = block.vineVariety,
                    phenologicalStage =
                        block.phenologicalStage,
                    latestActivityType =
                        block.latestActivityType,
                    latestActivityPriority =
                        block.latestActivityPriority,
                    latestActivityStatus =
                        block.latestActivityStatus,
                    activeDiseaseCount =
                        block.activeDiseaseCount,
                    activeAlertCount =
                        block.activeAlertCount
                )
            },

            activeDiseases =
                summary.activeDiseases.map { disease ->
                    DiseaseUiModel(
                        occurrenceId =
                            disease.occurrenceId,
                        diseaseId =
                            disease.diseaseId,
                        diseaseName =
                            disease.diseaseName,
                        blockId =
                            disease.blockId,
                        blockName =
                            disease.blockName,
                        observedAt =
                            disease.observedAt,
                        severity =
                            disease.severity,
                        status =
                            disease.status
                    )
                },

            activeAlerts =
                summary.activeAlerts.map { alert ->
                    AlertUiModel(
                        alertId =
                            alert.alertId,
                        diseaseId =
                            alert.diseaseId,
                        diseaseName =
                            alert.diseaseName,
                        blockId =
                            alert.blockId,
                        blockName =
                            alert.blockName,
                        createdAt =
                            alert.createdAt,
                        alert =
                            alert.alert,
                        status =
                            alert.status,
                        severity =
                            alert.severity,
                        phenologicalStage =
                            alert.phenologicalStage
                    )
                }
        )
    }
}