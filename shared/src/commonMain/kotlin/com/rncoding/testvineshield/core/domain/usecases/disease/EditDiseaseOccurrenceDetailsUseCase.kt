package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
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

class EditDiseaseOccurrenceDetailsUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val diseases: DiseaseRepository,
    private val clock: AppClock
) {
    suspend operator fun invoke(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long,
        observation: BlockDiseaseObservation,
        observedOn: LocalDate,
        severity: Int,
        notes: String
    ): Result<Unit, AppError> {
        if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
        val normalizedNotes = notes.trim().ifBlank { null }
        val curedAt = observation.curedAt
        if (curedAt != null && observedOn > curedAt) {
            return Result.Error(DiseaseError.InvalidInput("The observation date cannot be after its resolution date."))
        }

        return try {
            val now = clock.nowMillis()
            validateObservationDetails(vineyards, ownerId, vineyardId, observedOn, severity, normalizedNotes, now)
                ?.let { return Result.Error(it) }
            currentCoroutineContext().ensureActive()
            if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
            val changed = diseases.updateOwnedBlockOccurrenceDetails(
                ownerId, vineyardId, blockId, observation.id, observation.updatedAt,
                observedOn, severity, normalizedNotes, now
            )
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else if (changed) Result.Success(Unit)
            else Result.Error(DiseaseError.InvalidInput("This observation changed or is no longer available. Close and reopen it before retrying."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
