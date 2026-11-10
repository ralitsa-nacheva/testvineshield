package com.rncoding.testvineshield.core.domain.usecases.harvest

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.export.*
import com.rncoding.testvineshield.core.domain.repository.HarvestRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.usecases.block.GetBlockDetailsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class PrepareHarvestExportUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val getBlock: GetBlockDetailsUseCase,
    private val harvests: HarvestRepository,
    private val encoder: HarvestExportEncoder,
    private val clock: AppClock
) {
    suspend operator fun invoke(scope: HarvestScope, season: String?, format: HarvestExportFormat): Result<PreparedHarvestExport, AppError> {
        val owner = users.currentUserOrNull() ?: return Result.Error(AuthError.NoActiveSession)
        if (scope.vineyardId <= 0 || scope.blockId <= 0 || (season != null && season.isBlank()))
            return Result.Error(HarvestError.InvalidInput("Select a valid block and season scope."))
        return try {
            val vineyard = when (val result = vineyards.getVineyard(owner.userId, scope.vineyardId)) {
                is Result.Error -> return Result.Error(result.error)
                is Result.Success -> result.data
            }
            val block = when (val result = getBlock(scope.vineyardId, scope.blockId)) {
                is Result.Error -> return Result.Error(result.error)
                is Result.Success -> result.data
            }
            val records = harvests.observeHarvestsForBlock(scope.blockId).first()
                .filter { season == null || it.season == season }
            if (records.isEmpty()) return Result.Error(HarvestError.InvalidInput("No harvest records to export in this scope."))
            // Never silently include a repository row outside the requested block.
            if (records.any { it.blockId != scope.blockId }) return Result.Error(HarvestError.InvalidInput("Harvest scope changed. Reload and try again."))
            val document = encoder.encode(HarvestExportSnapshot(scope, vineyard.name, block.name, season, clock.nowMillis(), records), format)
            if (users.currentUserOrNull()?.userId != owner.userId) return Result.Error(AuthError.NoActiveSession)
            Result.Success(document)
        } catch (e: CancellationException) {
            throw e
        } catch (_: HarvestExportLimitException) {
            Result.Error(HarvestError.InvalidInput("Export is too large. Select a single season and try again."))
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
