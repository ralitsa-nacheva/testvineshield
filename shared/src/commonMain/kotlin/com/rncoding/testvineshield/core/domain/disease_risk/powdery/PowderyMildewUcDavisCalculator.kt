package com.rncoding.testvineshield.core.domain.disease_risk.powdery

class PowderyMildewUcDavisCalculator {

    data class Evaluation(
        val state: PowderyMildewRiskState,
        val dailyResult: PowderyMildewDailyRiskResult
    )

    fun evaluateDay(
        day: PowderyMildewDailyTemperature,
        previousState: PowderyMildewRiskState
    ): Evaluation {

        val qualifyingHours =
            longestConsecutiveConduciveHours(
                day.hourlyTemperaturesCelsius
            )

        val qualifyingDay =
            qualifyingHours >= REQUIRED_CONDUCIVE_HOURS

        val highTemperaturePenalty =
            hasHighTemperaturePenalty(
                day.hourlyTemperaturesCelsius
            )

        return if (!previousState.initiated) {
            evaluateBeforeInitiation(
                day = day,
                previousState = previousState,
                qualifyingHours = qualifyingHours,
                qualifyingDay = qualifyingDay
            )
        } else {
            evaluateAfterInitiation(
                day = day,
                previousState = previousState,
                qualifyingHours = qualifyingHours,
                qualifyingDay = qualifyingDay,
                highTemperaturePenalty =
                    highTemperaturePenalty
            )
        }
    }

    private fun evaluateBeforeInitiation(
        day: PowderyMildewDailyTemperature,
        previousState: PowderyMildewRiskState,
        qualifyingHours: Int,
        qualifyingDay: Boolean
    ): Evaluation {

        val consecutiveDays =
            if (qualifyingDay) {
                previousState.consecutiveQualifyingDays + 1
            } else {
                0
            }

        val initiated =
            consecutiveDays >= REQUIRED_TRIGGER_DAYS

        val index =
            if (initiated) {
                INITIAL_RISK_INDEX
            } else {
                0
            }

        val newState =
            previousState.copy(
                initiated = initiated,
                currentIndex = index,
                consecutiveQualifyingDays =
                    if (initiated) {
                        0
                    } else {
                        consecutiveDays
                    },
                lastEvaluatedDate = day.date
            )

        return Evaluation(
            state = newState,
            dailyResult =
                PowderyMildewDailyRiskResult(
                    date = day.date,
                    riskIndex = index,
                    qualifyingHours =
                        qualifyingHours,
                    qualifyingDay =
                        qualifyingDay,
                    highTemperaturePenalty =
                        false,
                    initiated = initiated
                )
        )
    }

    private fun evaluateAfterInitiation(
        day: PowderyMildewDailyTemperature,
        previousState: PowderyMildewRiskState,
        qualifyingHours: Int,
        qualifyingDay: Boolean,
        highTemperaturePenalty: Boolean
    ): Evaluation {

        var index =
            previousState.currentIndex

        if (qualifyingDay) {
            index += DAILY_INCREASE
        } else {
            index -= DAILY_DECREASE
        }

        if (highTemperaturePenalty) {
            index -= HIGH_TEMP_DECREASE
        }

        index =
            index.coerceIn(
                MIN_RISK_INDEX,
                MAX_RISK_INDEX
            )

        val newState =
            previousState.copy(
                initiated = true,
                currentIndex = index,
                consecutiveQualifyingDays = 0,
                lastEvaluatedDate = day.date
            )

        return Evaluation(
            state = newState,
            dailyResult =
                PowderyMildewDailyRiskResult(
                    date = day.date,
                    riskIndex = index,
                    qualifyingHours =
                        qualifyingHours,
                    qualifyingDay =
                        qualifyingDay,
                    highTemperaturePenalty =
                        highTemperaturePenalty,
                    initiated = true
                )
        )
    }

    private fun longestConsecutiveConduciveHours(
        temperatures: List<Double>
    ): Int {

        var longest = 0
        var current = 0

        for (temperature in temperatures) {

            val conducive =
                temperature >= MIN_CONDUCIVE_TEMP_C &&
                        temperature <= MAX_CONDUCIVE_TEMP_C

            if (conducive) {
                current += 1
                longest = maxOf(
                    longest,
                    current
                )
            } else {
                current = 0
            }
        }

        return longest
    }

    private fun hasHighTemperaturePenalty(
        temperatures: List<Double>
    ): Boolean {

        var consecutiveHours = 0

        for (temperature in temperatures) {

            if (temperature >= HIGH_TEMP_PENALTY_C) {
                consecutiveHours += 1

                if (
                    consecutiveHours >=
                    HIGH_TEMP_PENALTY_HOURS
                ) {
                    return true
                }
            } else {
                consecutiveHours = 0
            }
        }

        return false
    }

    companion object {

        const val MODEL_VERSION =
            "UC_DAVIS_GUBLER_THOMAS_HOURLY_REVISED_V1"

        const val MIN_CONDUCIVE_TEMP_C = 21.0
        const val MAX_CONDUCIVE_TEMP_C = 30.0

        const val REQUIRED_CONDUCIVE_HOURS = 6
        const val REQUIRED_TRIGGER_DAYS = 3

        const val INITIAL_RISK_INDEX = 60

        const val DAILY_INCREASE = 20
        const val DAILY_DECREASE = 10

        const val HIGH_TEMP_PENALTY_C = 38.0
        const val HIGH_TEMP_PENALTY_HOURS = 2
        const val HIGH_TEMP_DECREASE = 10

        const val MIN_RISK_INDEX = 0
        const val MAX_RISK_INDEX = 100
    }
}