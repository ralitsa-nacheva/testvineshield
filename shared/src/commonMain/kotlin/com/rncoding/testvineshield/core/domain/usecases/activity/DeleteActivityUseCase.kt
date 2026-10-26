
package com.rncoding.testvineshield.core.domain.usecases.activity

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDeletionOutcome
import com.rncoding.testvineshield.core.domain.error.ActivityError
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.ActivityRepository
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import kotlinx.coroutines.CancellationException

class DeleteActivityUseCase(
    private val activityRepository: ActivityRepository,
    private val vineyardRepository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    suspend operator fun invoke(
        vineyardId: Long,
        activityId: Long
    ): Result<Unit, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        if (vineyardId <= 0L || activityId <= 0L) {
            return Result.Error(
                ActivityError.InvalidInput(
                    "Invalid vineyard or activity ID."
                )
            )
        }

        return try {
            // Verify that the vineyard belongs to the
            // currently authenticated user.
            when (
                val vineyardResult = vineyardRepository.getVineyard(
                    userId = user.userId,
                    vineyardId = vineyardId
                )
            ) {
                is Result.Error -> {
                    return Result.Error(vineyardResult.error)
                }

                is Result.Success -> Unit
            }

            // The database deletion is scoped to the vineyard.

            return when (
                activityRepository.deleteActivity(
                    vineyardId = vineyardId,
                    activityId = activityId
                )
            ) {
                ActivityDeletionOutcome.DELETED ->
                    Result.Success(Unit)

                ActivityDeletionOutcome.NOT_FOUND ->
                    Result.Error(ActivityError.NotFound)

                ActivityDeletionOutcome.LINKED_TO_DISEASE ->
                    Result.Error(ActivityError.LinkedToDisease)
            }


        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
