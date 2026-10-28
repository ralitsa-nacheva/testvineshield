
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

class CreateHarvestUseCase(
    private val harvestRepository: HarvestRepository,
    private val blockRepository: BlockRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val validator: HarvestValidator
) {

    suspend operator fun invoke(
        harvest: HarvestDomainModel,
        vineyardId: Long
    ): Result<Long, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        if (vineyardId <= 0L || harvest.blockId <= 0L) {
            return Result.Error(
                HarvestError.InvalidInput("Invalid vineyard or block ID.")
            )
        }

        if (harvest.harvestId != 0L) {
            return Result.Error(
                HarvestError.InvalidInput(
                    "A new harvest must not have an existing ID."
                )
            )
        }

        val validationError = validator.validate(harvest)

        if (validationError != null) {
            return Result.Error(validationError)
        }

        return try {

            // Verify the selected vineyard belongs to the current user.
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

            // Verify the harvest's block belongs to that vineyard.
            val block = blockRepository.getBlock(
                vineyardId = vineyardId,
                blockId = harvest.blockId
            ) ?: return Result.Error(BlockError.NotFound)

            val newHarvestId = harvestRepository.createHarvest(
                harvest.copy(
                    blockId = block.blockId,
                    season = harvest.season.trim(),
                    units = harvest.units.trim()
                )
            )

            if (newHarvestId != null && newHarvestId > 0L) {
                Result.Success(newHarvestId)
            } else {
                Result.Error(
                    UnexpectedError(
                        IllegalStateException(
                            "Harvest insertion did not return a valid ID."
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
