
package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.error.ActivityError
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.ActivityRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import kotlinx.coroutines.CancellationException

class CreateActivityUseCase(
    private val activityRepository: ActivityRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    suspend operator fun invoke(
        activity: ActivityDomainModel
    ): Result<Unit, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        if (activity.activityId != 0L) {
            return Result.Error(
                ActivityError.InvalidInput(
                    "A new activity must not have an existing ID."
                )
            )
        }

        if (activity.vineyardId <= 0L) {
            return Result.Error(
                ActivityError.InvalidInput(
                    "Select a valid vineyard."
                )
            )
        }

        if (activity.activityType.isBlank()) {
            return Result.Error(
                ActivityError.InvalidInput(
                    "Activity type is required."
                )
            )
        }

        if (activity.season.isBlank()) {
            return Result.Error(
                ActivityError.InvalidInput(
                    "Season is required."
                )
            )
        }

        if (
            activity.completedAt != null &&
            activity.completedAt < activity.plannedAt
        ) {
            return Result.Error(
                ActivityError.InvalidInput(
                    "Completion date cannot precede the planned date."
                )
            )
        }

        // Check vineyard ownership using the authenticated user ID.
        when (
            val vineyardResult = vineyardRepository.getVineyard(
                userId = user.userId,
                vineyardId = activity.vineyardId
            )
        ) {
            is Result.Error -> {
                return Result.Error(vineyardResult.error)
            }

            is Result.Success -> Unit
        }

        try {
            // Block-scoped activities must reference a block
            // belonging to the selected vineyard.
            val blockId = activity.blockId

            if (blockId != null) {
                val validBlock = activityRepository.belongsToVineyard(
                    vineyardId = activity.vineyardId,
                    blockId = blockId
                )

                if (!validBlock) {
                    return Result.Error(ActivityError.InvalidBlock)
                }
            }

            activityRepository.addActivity(
                activity.copy(
                    activityType = activity.activityType.trim(),
                    activityDescription =
                        activity.activityDescription.trim(),
                    season = activity.season.trim()
                )
            )

            return Result.Success(Unit)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.Error(UnexpectedError(e))
        }
    }
}
