package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
import com.rncoding.testvineshield.core.domain.datamodels.ObservedReferenceSymptom
import com.rncoding.testvineshield.core.domain.datamodels.ReferenceSymptom
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

class RecordReferenceSymptomUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val diseases: DiseaseRepository,
    private val clock: AppClock
) {
    suspend operator fun invoke(
        ownerId: Long, vineyardId: Long, blockId: Long, observation: BlockDiseaseObservation,
        symptom: ReferenceSymptom, existing: ObservedReferenceSymptom?, observedOn: LocalDate, notes: String
    ): Result<Unit, AppError> {
        if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
        if (symptom.diseaseCode != observation.diseaseCode ||
            (existing != null && (existing.kind != symptom || existing !in observation.referenceSymptoms))
        ) return Result.Error(DiseaseError.InvalidInput("This symptom is unavailable for the observation."))
        val resolutionDate = observation.curedAt
        if (resolutionDate != null && observedOn > resolutionDate) {
            return Result.Error(DiseaseError.InvalidInput("The symptom date cannot be after the resolution date."))
        }
        val normalizedNotes = notes.trim()
        if (normalizedNotes.length > 4000) {
            return Result.Error(DiseaseError.InvalidInput("Use at most 4000 characters for notes."))
        }

        return try {
            validateObservationDate(vineyards, ownerId, vineyardId, observedOn, clock.nowMillis())
                ?.let { return Result.Error(it) }
            currentCoroutineContext().ensureActive()
            if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
            val recorded = if (existing == null) {
                diseases.recordOwnedReferenceSymptom(
                    ownerId, vineyardId, blockId, observation.id, observation.updatedAt, observedOn, normalizedNotes, symptom
                )
            } else {
                diseases.updateOwnedReferenceSymptom(
                    ownerId, vineyardId, blockId, observation.id, observation.updatedAt,
                    existing.symptomId, existing.observedOn, existing.notes, observedOn, normalizedNotes
                )
            }
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else if (recorded) Result.Success(Unit)
            else Result.Error(DiseaseError.InvalidInput("The observation or symptom changed. Close and reopen it."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }

    suspend fun remove(
        ownerId: Long, vineyardId: Long, blockId: Long, observation: BlockDiseaseObservation,
        existing: ObservedReferenceSymptom
    ): Result<Unit, AppError> {
        if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
        if (existing !in observation.referenceSymptoms) {
            return Result.Error(DiseaseError.InvalidInput("This observation has no matching recorded symptom."))
        }
        return try {
            currentCoroutineContext().ensureActive()
            val removed = diseases.removeOwnedReferenceSymptom(
                ownerId, vineyardId, blockId, observation.id, observation.updatedAt,
                existing.symptomId, existing.observedOn, existing.notes
            )
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else if (removed) Result.Success(Unit)
            else Result.Error(DiseaseError.InvalidInput("The observation or symptom changed. Close and reopen it."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
