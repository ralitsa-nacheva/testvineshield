package com.rncoding.testvineshield.core.presentation.vineyard_list

import com.rncoding.testvineshield.core.domain.datamodels.VineyardSummary

class VineyardListMapper {

    fun toUiModel(
        summary: VineyardSummary
    ): VineyardListItemUiModel {

        return VineyardListItemUiModel(
            vineyardId = summary.vineyardId,
            name = summary.name,
            location = listOf(
                summary.city,
                summary.country
            )
                .filter { it.isNotBlank() }
                .joinToString(", "),
            blockCount = summary.blockCount,
            latestTemperature =
                summary.latestTemperature,
            activeAlert =
                summary.activeAlert,
            currentDiseaseCount =
                summary.currentDiseaseCount,
            lastActivityPriority =
                summary.lastActivityPriority,
            lastActivityStatus =
                summary.lastActivityStatus
        )
    }
}