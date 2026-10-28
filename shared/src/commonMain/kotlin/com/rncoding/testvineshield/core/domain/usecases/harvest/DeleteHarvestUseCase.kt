
package com.rncoding.testvineshield.core.domain.usecases.harvest

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.error.HarvestError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import com.rncoding.testvineshield.core.domain.repository.HarvestRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import kotlinx.coroutines.CancellationException

class DeleteHarvestUseCase(
    private val harvestRepository: HarvestRepository,
    private val blockRepository: BlockRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    suspend operator fun invoke(
        vineyardId: Long,
        blockId: Long,
        harvestId: Long
    ): Result<Unit, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        if (
            vineyardId <= 0L ||
            blockId <= 0L ||
            harvestId <= 0L
        ) {
            return Result.Error(
                HarvestError.InvalidInput(
                    "Valid vineyard, block, and harvest IDs are required."
                )
            )
        }

        return try {

            // Verify vineyard ownership.
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

            // Verify block membership.
            val block = blockRepository.getBlock(
                vineyardId = vineyardId,
                blockId = blockId
            ) ?: return Result.Error(BlockError.NotFound)

            // Verify harvest membership.
            val existingHarvest = harvestRepository.getHarvest(
                blockId = block.blockId,
                harvestId = harvestId
            )

            if (existingHarvest == null) {
                return Result.Error(
                    HarvestError.NotFound
                )
            }

            // Delete using both block ID and harvest ID.
            val deleted = harvestRepository.deleteHarvest(
                blockId = blockId,
                harvestId = harvestId
            )

            if (deleted) {
                Result.Success(Unit)
            } else {
                Result.Error(
                    HarvestError.NotFound
                )
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
