package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelState

data class DownyMildewPrimaryState(
    val phase: Phase = Phase.WAITING_FOR_TRIGGER,
    val triggerStartedAt: Long? = null,
    val soilWetHours: Int = 0,
    val consecutiveDryHours: Int = 0,
    val germinationCompletedAt: Long? = null
) : DiseaseRiskModelState {

    enum class Phase {
        WAITING_FOR_TRIGGER,
        GERMINATION_IN_PROGRESS,
        GERMINATION_COMPLETE
    }
}