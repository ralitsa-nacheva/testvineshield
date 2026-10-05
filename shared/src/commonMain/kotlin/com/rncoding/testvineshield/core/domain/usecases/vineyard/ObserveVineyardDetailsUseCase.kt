package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockSummary
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertSummary
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseOccurrenceSummary
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDetailedSummary
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherRiskSummary
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherSummary
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ObserveVineyardDetailsUseCase(
    private val repository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(
        vineyardId: Long
    ): Flow<VineyardDetailedSummary?> {
        return authenticatedUserProvider
            .observeUser()
            .flatMapLatest { user ->
                if (user == null) {
                    emptyFlow()
                } else {
                    repository
                        .observeVineyard(
                            userId = user.userId,
                            vineyardId = vineyardId
                        )
                        .flatMapLatest { vineyard ->
                            if (vineyard == null) {
                                flowOf(null)
                            } else {
                                observeDetails(
                                    vineyard = vineyard,
                                    vineyardId = vineyardId
                                )
                            }
                        }
                }
            }
    }

    private fun observeDetails(
        vineyard: VineyardDomainModel,
        vineyardId: Long
    ): Flow<VineyardDetailedSummary?> {

        val environmentFlow =
            combine(
                repository.observeLatestWeather(
                    vineyardId
                ),
                repository.observeLatestWeatherRisk(
                    vineyardId
                ),
                repository.observeBlockSummaries(
                    vineyardId
                )
            ) { weather, risk, blocks ->
                EnvironmentDetails(
                    weather = weather,
                    risk = risk,
                    blocks = blocks
                )
            }

        val healthFlow =
            combine(
                repository.observeActiveDiseaseOccurrences(
                    vineyardId
                ),
                repository.observeActiveDiseaseAlerts(
                    vineyardId
                )
            ) { diseases, alerts ->
                HealthDetails(
                    diseases = diseases,
                    alerts = alerts
                )
            }

        return combine(
            environmentFlow,
            healthFlow
        ) { environment, health ->
            VineyardDetailedSummary(
                vineyard = vineyard,
                latestWeather =
                    environment.weather,
                latestWeatherRisk =
                    environment.risk,
                blocks =
                    environment.blocks,
                activeDiseases =
                    health.diseases,
                activeAlerts =
                    health.alerts
            )
        }
    }

    private data class EnvironmentDetails(
        val weather: VineyardWeatherSummary?,
        val risk: VineyardWeatherRiskSummary?,
        val blocks: List<BlockSummary>
    )

    private data class HealthDetails(
        val diseases: List<DiseaseOccurrenceSummary>,
        val alerts: List<DiseaseAlertSummary>
    )
}