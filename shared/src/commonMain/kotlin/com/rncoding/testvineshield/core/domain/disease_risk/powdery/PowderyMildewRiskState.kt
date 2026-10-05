package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelState
import kotlinx.datetime.LocalDate

data class PowderyMildewRiskState(
    val initiated: Boolean = false,
    val currentIndex: Int = 0,
    val consecutiveQualifyingDays: Int = 0,
    val lastEvaluatedDate: LocalDate? = null
): DiseaseRiskModelState