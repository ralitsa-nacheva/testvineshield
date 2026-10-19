package com.rncoding.testvineshield.core.domain.disease_risk


import kotlinx.datetime.LocalDate

interface DiseaseRiskWeatherWindowPolicy {

    fun getWindow(
        modelType: DiseaseRiskModelType,
        calculatedAt: Long,
        timeZone: String,
        monitoringStartDate: LocalDate?,
        previousState: DiseaseRiskModelState?
    ): DiseaseRiskWeatherWindow
}