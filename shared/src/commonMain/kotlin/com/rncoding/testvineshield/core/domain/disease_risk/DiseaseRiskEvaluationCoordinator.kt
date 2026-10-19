package com.rncoding.testvineshield.core.domain.disease_risk

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DiseaseRiskEvaluationCoordinator {

    private val mutex = Mutex()

    suspend fun <T> withExclusiveEvaluation(
        action: suspend () -> T
    ): T {
        return mutex.withLock {
            action()
        }
    }
}