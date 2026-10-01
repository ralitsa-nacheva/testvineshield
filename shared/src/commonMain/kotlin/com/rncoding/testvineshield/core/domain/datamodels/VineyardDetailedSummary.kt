package com.rncoding.testvineshield.core.domain.datamodels

data class VineyardDetailedSummary(
    val vineyard: VineyardDomainModel,
    val latestWeather: VineyardWeatherSummary?,
    val latestWeatherRisk: VineyardWeatherRiskSummary?,
    val blocks: List<BlockSummary>,
    val activeDiseases: List<DiseaseOccurrenceSummary>,
    val activeAlerts: List<DiseaseAlertSummary>
)