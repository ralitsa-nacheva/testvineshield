package com.rncoding.testvineshield.core.domain.usecases.block

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockStageObservation
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import kotlinx.coroutines.CancellationException

class GetRecentBlockStageObservationsUseCase(
    private val users: AuthenticatedUserProvider,
    private val blocks: BlockRepository
) {
    suspend operator fun invoke(vineyardId: Long, blockId: Long): Result<List<BlockStageObservation>, AppError> {
        val ownerId = users.currentUserOrNull()?.userId ?: return Result.Error(AuthError.NoActiveSession)
        return try {
            val observations = blocks.getRecentStageObservations(ownerId, vineyardId, blockId)
            if (users.currentUserOrNull()?.userId != ownerId) Result.Error(AuthError.NoActiveSession)
            else Result.Success(observations)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.Error(UnexpectedError(error))
        }
    }
}
