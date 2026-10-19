package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelState
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import com.rncoding.testvineshield.core.domain.disease_risk.downy.DownyMildewPrimaryState
import com.rncoding.testvineshield.core.domain.disease_risk.powdery.PowderyMildewRiskState
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class DiseaseRiskStateCodec(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }
) {

    data class EncodedState(
        val stateType: String,
        val statePayload: String
    )

    sealed interface DecodeResult {

        data class Success(
            val state: DiseaseRiskModelState
        ) : DecodeResult

        data class Failure(
            val reason: String
        ) : DecodeResult
    }

    fun encode(
        modelType: DiseaseRiskModelType,
        state: DiseaseRiskModelState
    ): EncodedState {

        return when (modelType) {

            DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS -> {
                require(state is PowderyMildewRiskState) {
                    "UC Davis model requires PowderyMildewRiskState"
                }

                val dto = PowderyStateDto(
                    initiated = state.initiated,
                    currentIndex = state.currentIndex,
                    consecutiveQualifyingDays =
                        state.consecutiveQualifyingDays,
                    lastEvaluatedDate =
                        state.lastEvaluatedDate?.toString()
                )

                EncodedState(
                    stateType = POWDERY_STATE_TYPE,
                    statePayload = json.encodeToString(dto)
                )
            }

            DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY -> {
                require(state is DownyMildewPrimaryState) {
                    "Downy primary model requires DownyMildewPrimaryState"
                }

                val dto = DownyPrimaryStateDto(
                    phase = state.phase.name,
                    triggerStartedAt = state.triggerStartedAt,
                    soilWetHours = state.soilWetHours,
                    consecutiveDryHours = state.consecutiveDryHours,
                    germinationCompletedAt = state.germinationCompletedAt,
                    lastProcessedWeatherTimestamp =
                        state.lastProcessedWeatherTimestamp
                )

                EncodedState(
                    stateType = DOWNY_PRIMARY_STATE_TYPE,
                    statePayload = json.encodeToString(dto)
                )
            }

            else -> throw IllegalArgumentException(
                "Model $modelType does not have persistent state"
            )
        }
    }

    fun decode(
        modelType: DiseaseRiskModelType,
        stateType: String,
        statePayload: String
    ): DecodeResult {

        return try {
            when (modelType) {

                DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS -> {
                    if (stateType != POWDERY_STATE_TYPE) {
                        return DecodeResult.Failure(
                            "Unexpected state type: $stateType"
                        )
                    }

                    val dto =
                        json.decodeFromString<PowderyStateDto>(
                            statePayload
                        )

                    if (
                        dto.currentIndex !in 0..100 ||
                        dto.consecutiveQualifyingDays < 0 ||
                        dto.consecutiveQualifyingDays > 2 ||
                        (!dto.initiated && dto.currentIndex != 0) ||
                        (dto.initiated &&
                                dto.consecutiveQualifyingDays != 0)
                    ) {
                        return DecodeResult.Failure(
                            "Invalid UC Davis state values"
                        )
                    }

                    val date = dto.lastEvaluatedDate?.let {
                        LocalDate.parse(it)
                    }

                    DecodeResult.Success(
                        PowderyMildewRiskState(
                            initiated = dto.initiated,
                            currentIndex = dto.currentIndex,
                            consecutiveQualifyingDays =
                                dto.consecutiveQualifyingDays,
                            lastEvaluatedDate = date
                        )
                    )
                }

                DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY -> {
                    if (stateType != DOWNY_PRIMARY_STATE_TYPE) {
                        return DecodeResult.Failure(
                            "Unexpected state type: $stateType"
                        )
                    }

                    val dto =
                        json.decodeFromString<DownyPrimaryStateDto>(
                            statePayload
                        )

                    val phase =
                        DownyMildewPrimaryState.Phase.entries
                            .firstOrNull {
                                it.name == dto.phase
                            }
                            ?: return DecodeResult.Failure(
                                "Unknown downy primary phase: ${dto.phase}"
                            )

                    if (
                        dto.soilWetHours < 0 ||
                        dto.consecutiveDryHours < 0 ||
                        dto.consecutiveDryHours > 3
                    ) {
                        return DecodeResult.Failure(
                            "Invalid downy primary state counters"
                        )
                    }

                    DecodeResult.Success(
                        DownyMildewPrimaryState(
                            phase = phase,
                            triggerStartedAt = dto.triggerStartedAt,
                            soilWetHours = dto.soilWetHours,
                            consecutiveDryHours = dto.consecutiveDryHours,
                            germinationCompletedAt = dto.germinationCompletedAt,
                            lastProcessedWeatherTimestamp =
                                dto.lastProcessedWeatherTimestamp
                        )
                    )
                }

                else -> DecodeResult.Failure(
                    "Model $modelType does not support persisted state"
                )
            }
        } catch (exception: SerializationException) {
            DecodeResult.Failure(
                "Invalid state JSON: ${exception.message}"
            )
        } catch (exception: IllegalArgumentException) {
            DecodeResult.Failure(
                "Invalid state value: ${exception.message}"
            )
        }
    }

    @Serializable
    private data class PowderyStateDto(
        val initiated: Boolean,
        val currentIndex: Int,
        val consecutiveQualifyingDays: Int,
        val lastEvaluatedDate: String?
    )

    @Serializable
    private data class DownyPrimaryStateDto(
        val phase: String,
        val triggerStartedAt: Long?,
        val soilWetHours: Int,
        val consecutiveDryHours: Int,
        val germinationCompletedAt: Long?,
        val lastProcessedWeatherTimestamp: Long?
    )

    companion object {
        const val POWDERY_STATE_TYPE =
            "powdery_uc_davis_state_v1"

        const val DOWNY_PRIMARY_STATE_TYPE =
            "downy_primary_state_v2"
    }
}