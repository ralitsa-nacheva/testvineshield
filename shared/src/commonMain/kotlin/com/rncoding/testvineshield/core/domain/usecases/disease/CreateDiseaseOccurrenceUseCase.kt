package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseOccurrenceDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.time.AppClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class CreateDiseaseOccurrenceUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val diseases: DiseaseRepository,
    private val clock: AppClock
) {
    suspend operator fun invoke(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        observedOn: LocalDate,
        severity: Int,
        notes: String
    ): Result<Long, AppError> {
        if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
        if (vineyardId <= 0 || blockId <= 0 || diseaseId <= 0) {
            return Result.Error(DiseaseError.InvalidInput("Choose an available block and disease."))
        }
        if (severity !in 0..100) return Result.Error(DiseaseError.InvalidInput("Enter an affected percentage from 0 to 100."))
        val normalizedNotes = notes.trim()
        if (normalizedNotes.length > 4000) {
            return Result.Error(DiseaseError.InvalidInput("Use at most 4000 characters for notes."))
        }

        return try {
            val vineyard = when (val result = vineyards.getVineyard(ownerId, vineyardId)) {
                is Result.Error -> return result
                is Result.Success -> result.data
            }
            val zone = try {
                TimeZone.of(vineyard.timeZone)
            } catch (_: IllegalArgumentException) {
                return Result.Error(DiseaseError.InvalidInput("Set a valid vineyard time zone before recording an observation."))
            }
            val now = clock.nowMillis()
            if (observedOn > Instant.fromEpochMilliseconds(now).toLocalDateTime(zone).date) {
                return Result.Error(DiseaseError.InvalidInput("The observation date cannot be in the future for this vineyard."))
            }
            currentCoroutineContext().ensureActive()
            if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
            val id = diseases.createOwnedBlockOccurrence(
                ownerId,
                DiseaseOccurrenceDomainModel(
                    occurrenceId = 0,
                    diseaseId = diseaseId,
                    vineyardId = vineyardId,
                    blockId = blockId,
                    observedAt = observedOn,
                    severity = severity,
                    status = DiseaseOccurrenceStatus.ACTIVE,
                    curedAt = null,
                    createdAt = now,
                    updatedAt = now,
                    notes = normalizedNotes.ifBlank { null }
                )
            ) ?: return Result.Error(DiseaseError.InvalidInput("The block or built-in disease is no longer available."))
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else Result.Success(id)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
