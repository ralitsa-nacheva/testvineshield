package com.rncoding.testvineshield.core.domain.usecases.block

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.BlockError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/** Parent ownership is observed, not checked just once before an unscoped child stream. */
class ObserveOwnedBlocksUseCase(
private val blocks: BlockRepository, private val vineyards: VineyardRepository, private val users: AuthenticatedUserProvider
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(vineyardId: Long): Flow<Result<List<BlockDomainModel>, AppError>> {
        if (vineyardId <= 0) return flowOf(Result.Error(BlockError.InvalidInput("Invalid vineyard ID.")))
        return users.observeUser().flatMapLatest { user ->
            if (user == null) flowOf<Result<List<BlockDomainModel>, AppError>>(Result.Error(AuthError.NoActiveSession))
            else vineyards.observeVineyard(user.userId, vineyardId).flatMapLatest { parent ->
                if (parent == null) flowOf<Result<List<BlockDomainModel>, AppError>>(Result.Error(DatabaseError.NotFound("Vineyard")))
                else blocks.observeBlocks(vineyardId).map<List<BlockDomainModel>, Result<List<BlockDomainModel>, AppError>> { Result.Success(it) }
            }
        }.buffer(0).catch { error ->
            if (error is CancellationException) throw error
            emit(Result.Error(UnexpectedError()))
        }
    }
}
