package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes
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

class RecordOilSpotUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val diseases: DiseaseRepository,
    private val clock: AppClock
) {
    suspend operator fun invoke(
        ownerId: Long, vineyardId: Long, blockId: Long, observation: BlockDiseaseObservation,
        observedOn: LocalDate, notes: String
    ): Result<Unit, AppError> {
        if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
        if (observation.diseaseCode != DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW || observation.oilSpot != null) {
            return Result.Error(DiseaseError.InvalidInput("This observation cannot accept an oil spot record."))
        }
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
            val recorded = diseases.recordOwnedOilSpot(
                ownerId, vineyardId, blockId, observation.id, observation.updatedAt, observedOn, normalizedNotes
            )
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else if (recorded) Result.Success(Unit)
            else Result.Error(DiseaseError.InvalidInput("The record changed or already has an oil spot. Close and reopen it."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
