
package com.rncoding.testvineshield.core.domain.usecases.block

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import kotlinx.coroutines.CancellationException

class GetBlockDetailsUseCase(
    private val blockRepository: BlockRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    suspend operator fun invoke(
        vineyardId: Long,
        blockId: Long
    ): Result<BlockDomainModel, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        if (vineyardId <= 0L || blockId <= 0L) {
            return Result.Error(
                BlockError.InvalidInput(
                    "Invalid vineyard or block ID."
                )
            )
        }

        return try {
            when (
                val vineyardResult = vineyardRepository.getVineyard(
                    userId = user.userId,
                    vineyardId = vineyardId
                )
            ) {
                is Result.Error ->
                    return Result.Error(vineyardResult.error)

                is Result.Success -> Unit
            }

            val block = blockRepository.getBlock(
                vineyardId = vineyardId,
                blockId = blockId
            )

            if (block == null) {
                Result.Error(BlockError.NotFound)
            } else {
                Result.Success(block)
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
