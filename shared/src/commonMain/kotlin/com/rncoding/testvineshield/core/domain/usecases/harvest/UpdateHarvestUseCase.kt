
package com.rncoding.testvineshield.core.domain.usecases.harvest

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.error.HarvestError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import com.rncoding.testvineshield.core.domain.repository.HarvestRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.validation.HarvestValidator
import kotlinx.coroutines.CancellationException

class UpdateHarvestUseCase(
    private val harvestRepository: HarvestRepository,
    private val blockRepository: BlockRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val validator: HarvestValidator
) {

    suspend operator fun invoke(
        harvest: HarvestDomainModel,
        vineyardId: Long
    ): Result<Unit, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        if (
            vineyardId <= 0L ||
            harvest.blockId <= 0L ||
            harvest.harvestId <= 0L
        ) {
            return Result.Error(
                HarvestError.InvalidInput(
                    "Valid vineyard, block, and harvest IDs are required."
                )
            )
        }

        val validationError = validator.validate(harvest)

        if (validationError != null) {
            return Result.Error(validationError)
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

            // Verify the block belongs to this vineyard.
            val block = blockRepository.getBlock(
                vineyardId = vineyardId,
                blockId = harvest.blockId
            ) ?: return Result.Error(BlockError.NotFound)

            // Verify this harvest belongs to the selected block.
            val existingHarvest = harvestRepository.getHarvest(
                blockId = block.blockId,
                harvestId = harvest.harvestId
            )

            if (existingHarvest == null) {
                return Result.Error(
                    HarvestError.NotFound)
            }

            // Normalize fields and perform the atomic update.
            val updated = harvestRepository.updateHarvest(
                harvest.copy(
                    season = harvest.season.trim(),
                    units = harvest.units.trim()
                )
            )

            if (updated) {
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
