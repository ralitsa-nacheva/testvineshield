package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.WeatherCalculationEntity
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherRiskSummary

//temporary before adding disease models

class VineyardWeatherRiskSummaryMapper {

    fun toDomain(
        entity: WeatherCalculationEntity
    ): VineyardWeatherRiskSummary {
        return VineyardWeatherRiskSummary(
            calculatedAt = entity.calculatedAt,
            infectionScore = entity.infectionScore,
            powderyMildewInitialInfection =
                entity.powderyMildewInitialInfection,
            wetnessHoursAt10C =
                entity.countWetnessHoursAt10C,
            hoursAt21C =
                entity.countHoursAt21C,
            daysFromBudBurst =
                entity.daysFromBudBurst
        )
    }
}