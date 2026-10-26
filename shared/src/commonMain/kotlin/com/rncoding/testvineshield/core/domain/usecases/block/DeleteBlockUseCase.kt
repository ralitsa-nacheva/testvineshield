
package com.rncoding.testvineshield.core.domain.usecases.block

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import kotlinx.coroutines.CancellationException

class DeleteBlockUseCase(
    private val blockRepository: BlockRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    suspend operator fun invoke(
        vineyardId: Long,
        blockId: Long
    ): Result<Unit, AppError> {

        // 1. Check authentication
        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        // 2. Validate IDs
        if (vineyardId <= 0L || blockId <= 0L) {
            return Result.Error(
                BlockError.InvalidInput(
                    "Invalid vineyard or block ID."
                )
            )
        }

        return try {

            // 3. Verify vineyard ownership
            when (
                val vineyardResult = vineyardRepository.getVineyard(
                    userId = user.userId,
                    vineyardId = vineyardId
                )
            ) {
                is Result.Error -> {
                    return Result.Error(vineyardResult.error)
                }

                is Result.Success -> Unit
            }

            // 4. Verify the block exists in this vineyard
            val existingBlock = blockRepository.getBlock(
                vineyardId = vineyardId,
                blockId = blockId
            )

            if (existingBlock == null) {
                return Result.Error(BlockError.NotFound)
            }

            // 5. Delete the block
            val deleted = blockRepository.deleteBlock(
                vineyardId = vineyardId,
                blockId = blockId
            )

            if (deleted) {
                Result.Success(Unit)
            } else {
                Result.Error(BlockError.NotFound)
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
