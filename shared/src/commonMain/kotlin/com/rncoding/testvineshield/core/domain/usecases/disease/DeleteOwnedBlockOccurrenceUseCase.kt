package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class DeleteOwnedBlockOccurrenceUseCase(
    private val users: AuthenticatedUserProvider,
    private val diseases: DiseaseRepository
) {
    suspend operator fun invoke(
        ownerId: Long, vineyardId: Long, blockId: Long, observation: BlockDiseaseObservation
    ): Result<Unit, AppError> {
        return try {
            currentCoroutineContext().ensureActive()
            if (users.currentUserOrNull()?.userId != ownerId) return Result.Error(AuthError.NoActiveSession)
            val deleted = diseases.deleteOwnedBlockOccurrence(
                ownerId, vineyardId, blockId, observation.id, observation.updatedAt
            )
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else if (deleted) Result.Success(Unit)
            else Result.Error(DiseaseError.InvalidInput("The observation changed or is no longer available. Close and reopen it."))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
