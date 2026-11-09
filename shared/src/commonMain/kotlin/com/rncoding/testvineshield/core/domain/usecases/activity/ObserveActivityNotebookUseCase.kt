package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/** Zero vineyard selects the account's vineyard chooser; zero block selects all vineyard work. */
data class ActivityScope(val vineyardId: Long = 0, val blockId: Long = 0)
data class ActivityVineyardChoice(val id: Long, val name: String)
data class ActivityNotebookSnapshot(
    val vineyards: List<ActivityVineyardChoice> = emptyList(),
    val title: String = "Activities",
    val blocks: List<BlockDomainModel> = emptyList(),
    val activities: List<ActivityDomainModel> = emptyList()
)
class ObserveActivityNotebookUseCase(
    private val users: AuthenticatedUserProvider,
    private val vineyards: VineyardRepository,
    private val blocks: BlockRepository,
    private val activities: ActivityRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(scope: ActivityScope): Flow<Result<ActivityNotebookSnapshot, AppError>> {
        if (scope.vineyardId < 0 || scope.blockId < 0 || (scope.vineyardId == 0L && scope.blockId != 0L))
            return flowOf(Result.Error(ActivityError.InvalidInput("Invalid activity scope.")))
        return users.observeUser().flatMapLatest { user ->
            if (user == null) flowOf<Result<ActivityNotebookSnapshot, AppError>>(Result.Error(AuthError.NoActiveSession))
            else if (scope.vineyardId == 0L) vineyards.observeVineyards(user.userId)
                .map<List<VineyardDomainModel>, Result<ActivityNotebookSnapshot, AppError>> { parents ->
                    Result.Success(ActivityNotebookSnapshot(vineyards = parents.map { ActivityVineyardChoice(it.vineyardId,it.name) }))
                }
            else vineyards.observeVineyard(user.userId,scope.vineyardId).flatMapLatest { parent ->
                if (parent == null) flowOf<Result<ActivityNotebookSnapshot, AppError>>(Result.Error(DatabaseError.NotFound("Vineyard")))
                else combine(blocks.observeBlocks(scope.vineyardId),activities.observeActivities(scope.vineyardId)) { children, records ->
                    val block = children.firstOrNull { it.blockId == scope.blockId }
                    val result: Result<ActivityNotebookSnapshot, AppError> =
                        if (scope.blockId != 0L && block == null) Result.Error(ActivityError.InvalidBlock)
                        else Result.Success(ActivityNotebookSnapshot(title = block?.name ?: parent.name, blocks = children,
                            activities = records.filter { scope.blockId == 0L || it.blockId == scope.blockId }))
                    result
                }
            }
        }.buffer(0).catch { error ->
            if (error is CancellationException) throw error
            emit(Result.Error(UnexpectedError()))
        }
    }
}
