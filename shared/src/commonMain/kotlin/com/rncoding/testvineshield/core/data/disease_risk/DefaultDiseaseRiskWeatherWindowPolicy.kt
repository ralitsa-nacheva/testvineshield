package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelState
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherWindow
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherWindowPolicy
import com.rncoding.testvineshield.core.domain.disease_risk.downy.DownyMildewPrimaryState
import com.rncoding.testvineshield.core.domain.disease_risk.powdery.PowderyMildewRiskState
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class DefaultDiseaseRiskWeatherWindowPolicy :
    DiseaseRiskWeatherWindowPolicy {

    private companion object {
        const val ONE_HOUR_MILLIS = 3_600_000L
        const val STATELESS_WINDOW_HOURS = 24L
    }

    override fun getWindow(
        modelType: DiseaseRiskModelType,
        calculatedAt: Long,
        timeZone: String,
        monitoringStartDate: LocalDate?,
        previousState: DiseaseRiskModelState?
    ): DiseaseRiskWeatherWindow {

        val zone = TimeZone.of(timeZone)

        return when (modelType) {

            DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS ->
                getUcDavisWindow(
                    calculatedAt = calculatedAt,
                    timeZone = zone,
                    monitoringStartDate = monitoringStartDate,
                    previousState = previousState
                )

            DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY ->
                getDownyPrimaryWindow(
                    calculatedAt = calculatedAt,
                    timeZone = zone,
                    monitoringStartDate = monitoringStartDate,
                    previousState = previousState
                )

            DiseaseRiskModelType.POWDERY_MILDEW_ASCOSPORE,
            DiseaseRiskModelType.DOWNY_MILDEW_SECONDARY,
            DiseaseRiskModelType.BOTRYTIS ->
                getStatelessWindow(
                    calculatedAt = calculatedAt,
                    timeZone = zone,
                    previousState = previousState
                )
        }
    }

    private fun getUcDavisWindow(
        calculatedAt: Long,
        timeZone: TimeZone,
        monitoringStartDate: LocalDate?,
        previousState: DiseaseRiskModelState?
    ): DiseaseRiskWeatherWindow {

        val startDate = when (previousState) {

            null ->
                requireNotNull(monitoringStartDate) {
                    "UC Davis monitoring requires a start date."
                }

            is PowderyMildewRiskState -> {
                val lastDate = requireNotNull(
                    previousState.lastEvaluatedDate
                ) {
                    "Existing UC Davis state has no last evaluated date."
                }

                lastDate.plus(1, DateTimeUnit.DAY)
            }

            else ->
                throw IllegalArgumentException(
                    "Incompatible UC Davis model state."
                )
        }

        val startTimestamp = startDate
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()

        val currentLocalDate = Instant
            .fromEpochMilliseconds(calculatedAt)
            .toLocalDateTime(timeZone)
            .date

        // Only complete local calendar days are eligible.
        val endTimestamp = currentLocalDate
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()

        if (endTimestamp <= startTimestamp) {
            return DiseaseRiskWeatherWindow(
                startTimestamp = startTimestamp,
                endTimestamp = startTimestamp
            )
        }

        return DiseaseRiskWeatherWindow(
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )
    }

    private fun getDownyPrimaryWindow(
        calculatedAt: Long,
        timeZone: TimeZone,
        monitoringStartDate: LocalDate?,
        previousState: DiseaseRiskModelState?
    ): DiseaseRiskWeatherWindow {

        val startTimestamp = when (previousState) {

            null -> {
                val startDate = requireNotNull(monitoringStartDate) {
                    "Downy mildew primary monitoring requires a start date."
                }

                // Preserve vineyard-local midnight.
                startDate
                    .atStartOfDayIn(timeZone)
                    .toEpochMilliseconds()
            }

            is DownyMildewPrimaryState -> {
                val lastTimestamp =
                    previousState.lastProcessedWeatherTimestamp

                if (lastTimestamp != null) {
                    require(
                        lastTimestamp <= Long.MAX_VALUE - ONE_HOUR_MILLIS
                    ) {
                        "Invalid downy mildew weather cursor."
                    }

                    lastTimestamp + ONE_HOUR_MILLIS
                } else {
                    val startDate = requireNotNull(monitoringStartDate) {
                        "A monitoring start date is required when the weather cursor is absent."
                    }

                    // No cursor: resume from vineyard-local midnight.
                    startDate
                        .atStartOfDayIn(timeZone)
                        .toEpochMilliseconds()
                }
            }

            else ->
                throw IllegalArgumentException(
                    "Incompatible downy mildew primary model state."
                )
        }

        // Find the current vineyard-local date.
        val currentLocalDate = Instant
            .fromEpochMilliseconds(calculatedAt)
            .toLocalDateTime(timeZone)
            .date

        // Use local midnight as the hourly grid origin.
        val gridOrigin = currentLocalDate
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()

        // End is exclusive. Exclude the current incomplete hour.
        val elapsedMillis = calculatedAt - gridOrigin

        val endTimestamp =
            gridOrigin +
                    elapsedMillis.floorDiv(ONE_HOUR_MILLIS) *
                    ONE_HOUR_MILLIS

        if (endTimestamp <= startTimestamp) {
            return DiseaseRiskWeatherWindow(
                startTimestamp = startTimestamp,
                endTimestamp = startTimestamp
            )
        }

        return DiseaseRiskWeatherWindow(
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )
    }

    private fun getStatelessWindow(
        calculatedAt: Long,
        timeZone: TimeZone,
        previousState: DiseaseRiskModelState?
    ): DiseaseRiskWeatherWindow {

        require(previousState == null) {
            "Stateless disease-risk models must not have persisted state."
        }

        val windowDurationMillis =
            STATELESS_WINDOW_HOURS * ONE_HOUR_MILLIS

        // Find the current vineyard-local date.
        val currentLocalDate = Instant
            .fromEpochMilliseconds(calculatedAt)
            .toLocalDateTime(timeZone)
            .date

        // Align to the hourly grid used by the vineyard.
        val gridOrigin = currentLocalDate
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()

        // Exclude the current incomplete hourly interval.
        val elapsedMillis = calculatedAt - gridOrigin

        val endTimestamp =
            gridOrigin +
                    elapsedMillis.floorDiv(ONE_HOUR_MILLIS) *
                    ONE_HOUR_MILLIS

        require(
            endTimestamp >= Long.MIN_VALUE + windowDurationMillis
        ) {
            "Invalid timestamp for stateless weather window."
        }

        val startTimestamp =
            endTimestamp - windowDurationMillis

        return DiseaseRiskWeatherWindow(
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp
        )
    }
}