
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
import com.rncoding.testvineshield.core.domain.validation.BlockValidator
import kotlinx.coroutines.CancellationException

class UpdateBlockUseCase(
    private val blockRepository: BlockRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val validator: BlockValidator
) {

    suspend operator fun invoke(
        block: BlockDomainModel
    ): Result<Unit, AppError> {

        // 1. Check authentication
        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        // 2. Validate block fields
        val validationError = validator.validate(block)

        if (validationError != null) {
            return Result.Error(validationError)
        }

        // 3. An update requires an existing block ID
        if (block.blockId <= 0L) {
            return Result.Error(
                BlockError.InvalidInput(
                    "A valid block ID is required to update a block."
                )
            )
        }

        return try {

            // 4. Verify vineyard ownership
            when (
                val vineyardResult = vineyardRepository.getVineyard(
                    userId = user.userId,
                    vineyardId = block.vineyardId
                )
            ) {
                is Result.Error -> {
                    return Result.Error(vineyardResult.error)
                }

                is Result.Success -> Unit
            }

            // 5. Verify the block exists in this vineyard
            val existingBlock = blockRepository.getBlock(
                vineyardId = block.vineyardId,
                blockId = block.blockId
            )

            if (existingBlock == null) {
                return Result.Error(BlockError.NotFound)
            }

            // 6. Normalize text fields
            val normalizedBlock = block.copy(
                name = block.name.trim(),
                vineVariety = block.vineVariety.trim(),
                color = block.color.trim(),
                rootStock = block.rootStock.trim()
            )

            // 7. Update the existing block
            val updated = blockRepository.updateBlock(normalizedBlock)

            if (updated) {
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
