package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal suspend fun validateObservationDetails(
    vineyards: VineyardRepository,
    ownerId: Long,
    vineyardId: Long,
    observedOn: LocalDate,
    severity: Int,
    notes: String?,
    now: Long
): AppError? {
    if (severity !in 0..100) return DiseaseError.InvalidInput("Enter an affected percentage from 0 to 100.")
    if (notes != null && notes.length > 4000) return DiseaseError.InvalidInput("Use at most 4000 characters for notes.")
    return validateObservationDate(vineyards, ownerId, vineyardId, observedOn, now)
}

internal suspend fun validateObservationDate(
    vineyards: VineyardRepository,
    ownerId: Long,
    vineyardId: Long,
    observedOn: LocalDate,
    now: Long
): AppError? {
    val vineyard = when (val result = vineyards.getVineyard(ownerId, vineyardId)) {
        is Result.Error -> return result.error
        is Result.Success -> result.data
    }
    val zone = try {
        TimeZone.of(vineyard.timeZone)
    } catch (_: IllegalArgumentException) {
        return DiseaseError.InvalidInput("Set a valid vineyard time zone before saving an observation.")
    }
    val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date
    return if (observedOn > today) {
        DiseaseError.InvalidInput("The observation date cannot be in the future for this vineyard.")
    } else null
}
