
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

class CreateBlockUseCase(
    private val blockRepository: BlockRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val validator: BlockValidator
) {

    suspend operator fun invoke(
        block: BlockDomainModel
    ): Result<Long, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        val validationError = validator.validate(block)

        if (validationError != null) {
            return Result.Error(validationError)
        }

        if (block.blockId != 0L) {
            return Result.Error(
                BlockError.InvalidInput(
                    "A new block must not have an existing ID."
                )
            )
        }

        return try {
            when (
                val vineyardResult = vineyardRepository.getVineyard(
                    userId = user.userId,
                    vineyardId = block.vineyardId
                )
            ) {
                is Result.Error ->
                    return Result.Error(vineyardResult.error)

                is Result.Success -> Unit
            }

            val newBlockId = blockRepository.createBlock(
                block.copy(
                    name = block.name.trim(),
                    vineVariety = block.vineVariety.trim(),
                    color = block.color.trim(),
                    rootStock = block.rootStock.trim()
                )
            )

            if (newBlockId != null && newBlockId > 0L) {
                Result.Success(newBlockId)
            } else {
                Result.Error(
                    UnexpectedError(
                        IllegalStateException(
                            "Block insertion did not return a valid ID."
                        )
                    )
                )
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
