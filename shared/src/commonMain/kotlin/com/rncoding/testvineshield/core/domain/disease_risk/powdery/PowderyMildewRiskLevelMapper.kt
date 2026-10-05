package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskLevel

object PowderyMildewRiskLevelMapper {

    fun map(
        index: Int,
        initiated: Boolean
    ): DiseaseRiskLevel {

        if (!initiated) {
            return DiseaseRiskLevel.NONE
        }

        return when {
            index <= 30 ->
                DiseaseRiskLevel.LOW

            index <= 50 ->
                DiseaseRiskLevel.MODERATE

            else ->
                DiseaseRiskLevel.HIGH
        }
    }
}