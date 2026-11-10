package com.rncoding.testvineshield.core.domain.usecases.harvest

import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.HarvestRepository
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import kotlinx.coroutines.CancellationException

class GetHarvestDetailsUseCase(
    private val getBlock: GetBlockDetailsUseCase,
    private val harvests: HarvestRepository
) {
    suspend operator fun invoke(scope: HarvestScope, harvestId: Long): Result<HarvestDomainModel, AppError> {
        if (harvestId <= 0) return Result.Error(HarvestError.InvalidInput("Select a valid harvest."))
        return try {
            when (val block = getBlock(scope.vineyardId, scope.blockId)) {
                is Result.Error -> Result.Error(block.error)
                is Result.Success -> {
                    val record = harvests.getHarvest(scope.blockId, harvestId)
                    if (record == null) Result.Error(HarvestError.NotFound) else Result.Success(record)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
