
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

class UpdateActivityUseCase(
    private val activityRepository: ActivityRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    suspend operator fun invoke(
        activity: ActivityDomainModel
    ): Result<Unit, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        if (activity.activityId <= 0L || activity.vineyardId <= 0L) {
            return Result.Error(
                ActivityError.InvalidInput(
                    "Invalid activity or vineyard ID."
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

        return try {
            // Verify ownership of the vineyard.
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

            // Preserve creation metadata and avoid trusting
            // client-supplied timestamps for an existing row.
            val existing = activityRepository.getActivity(
                vineyardId = activity.vineyardId,
                activityId = activity.activityId
            ) ?: return Result.Error(ActivityError.NotFound)

            val updated = activity.copy(
                activityType = activity.activityType.trim(),
                activityDescription = activity.activityDescription.trim(),
                season = activity.season.trim(),
                createdAt = existing.createdAt,
                updatedAt = existing.updatedAt
            )

            val success = activityRepository.updateActivity(
                vineyardId = activity.vineyardId,
                activity = updated
            )

            if (success) {
                Result.Success(Unit)
            } else {
                Result.Error(ActivityError.NotFound)
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
