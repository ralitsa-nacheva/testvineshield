package com.rncoding.testvineshield.core.domain.usecases.harvest

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class HarvestScope(val vineyardId: Long, val blockId: Long)
data class HarvestNotebookSnapshot(val blockName: String, val records: List<HarvestDomainModel>)

class ObserveOwnedHarvestsUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val blocks: BlockRepository,
    private val harvests: HarvestRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(scope: HarvestScope): Flow<Result<HarvestNotebookSnapshot, AppError>> {
        if (scope.vineyardId <= 0 || scope.blockId <= 0) {
            return flowOf(Result.Error(HarvestError.InvalidInput("Select a valid vineyard and block.")))
        }
        return users.observeUser().flatMapLatest { user ->
            if (user == null) flowOf<Result<HarvestNotebookSnapshot, AppError>>(Result.Error(AuthError.NoActiveSession))
            else vineyards.observeVineyard(user.userId, scope.vineyardId).flatMapLatest { parent ->
                if (parent == null) flowOf<Result<HarvestNotebookSnapshot, AppError>>(Result.Error(DatabaseError.NotFound("Vineyard")))
                else blocks.observeBlocks(scope.vineyardId).flatMapLatest { children ->
                    val block = children.firstOrNull { it.blockId == scope.blockId }
                    if (block == null) flowOf<Result<HarvestNotebookSnapshot, AppError>>(Result.Error(BlockError.NotFound))
                    else harvests.observeHarvestsForBlock(scope.blockId)
                        .map<List<HarvestDomainModel>, Result<HarvestNotebookSnapshot, AppError>> { records ->
                            Result.Success(HarvestNotebookSnapshot(block.name, records))
                        }
                }
            }
        }.buffer(0).catch { error ->
            if (error is CancellationException) throw error
            emit(Result.Error(UnexpectedError()))
        }
    }
}
