package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.*
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone

class DiseaseRiskInputFactoryImpl(
    private val monitoringRepository: DiseaseRiskMonitoringConfigurationRepository,
    private val vineyardContextProvider: DiseaseRiskVineyardContextProvider,
    private val weatherWindowPolicy: DiseaseRiskWeatherWindowPolicy,
    private val weatherProvider: DiseaseRiskWeatherProvider,
    private val hostContextProvider: DiseaseRiskHostContextProvider
) : DiseaseRiskInputFactory {

    override suspend fun createInput(
        modelType: DiseaseRiskModelType,
        diseaseId: Long,
        vineyardId: Long,
        blockId: Long,
        calculatedAt: Long,
        previousState: DiseaseRiskModelState?
    ): DiseaseRiskInputResult {

        try {
            // 1. Retrieve monitoring configuration.
            val configuration = monitoringRepository.getConfiguration(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                modelType = modelType
            ) ?: return DiseaseRiskInputResult.MissingMonitoringConfiguration(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId,
                modelType = modelType
            )

            if (!configuration.enabled) {
                return DiseaseRiskInputResult.MonitoringDisabled(
                    vineyardId = vineyardId,
                    blockId = blockId,
                    diseaseId = diseaseId,
                    modelType = modelType
                )
            }

            // 2. Retrieve vineyard time zone.
            val timeZoneString = vineyardContextProvider.getTimeZone(
                vineyardId = vineyardId
            ) ?: return DiseaseRiskInputResult.MissingVineyard(
                vineyardId = vineyardId
            )

            try {
                TimeZone.of(timeZoneString)
            } catch (_: IllegalArgumentException) {
                return DiseaseRiskInputResult.InvalidTimeZone(
                    timeZone = timeZoneString
                )
            }

            // 3. Calculate weather window.
            val window = weatherWindowPolicy.getWindow(
                modelType = modelType,
                calculatedAt = calculatedAt,
                timeZone = timeZoneString,
                monitoringStartDate = configuration.monitoringStartDate,
                previousState = previousState
            )

            if (window.startTimestamp >= window.endTimestamp) {
                return DiseaseRiskInputResult.NoNewWeather
            }

            // 4. Retrieve historical weather.
            val hourlyWeather = weatherProvider.getHistoricalWeather(
                vineyardId = vineyardId,
                startTimestamp = window.startTimestamp,
                endTimestamp = window.endTimestamp
            )

            val weatherValidationError =
                HourlyWeatherContinuityValidator.validateWindowCompleteness(
                    weather = hourlyWeather,
                    window = window
                )

            if (weatherValidationError != null) {
                return DiseaseRiskInputResult.InsufficientWeather(
                    reason = weatherValidationError
                )
            }

            // 5. Retrieve host context.
            val hostContext = hostContextProvider.getHostContext(
                vineyardId = vineyardId,
                blockId = blockId,
                diseaseId = diseaseId
            )

            // 6. Construct the model input.
            val input = DiseaseRiskInput(
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = blockId,
                timeZone = timeZoneString,
                hourlyWeather = hourlyWeather,
                dailyWeather = emptyList(),
                hostContext = hostContext,
                monitoringStartDate = configuration.monitoringStartDate
            )

            return DiseaseRiskInputResult.Success(input)

        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            return DiseaseRiskInputResult.Failure(
                reason = exception.message
                    ?: "Unable to construct disease-risk input."
            )
        }
    }
}