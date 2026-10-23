
package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler
import kotlinx.coroutines.CancellationException

class DeleteVineyardUseCase(
    private val repository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val weatherHistoryRiskScheduler: WeatherHistoryRiskScheduler
) {

    suspend operator fun invoke(
        vineyardId: Long
    ): Result<Unit, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        val deleteResult = repository.deleteVineyard(
            userId = user.userId,
            vineyardId = vineyardId
        )

        return when (deleteResult) {

            is Result.Error -> {
                Result.Error(deleteResult.error)
            }

            is Result.Success -> {

                try {
                    weatherHistoryRiskScheduler.cancel(
                        userId = user.userId,
                        vineyardId = vineyardId
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The vineyard was already deleted successfully.
                    // Scheduling cancellation is best-effort.
                    // Add diagnostic logging/recovery in a later checkpoint.
                }

                Result.Success(Unit)
            }
        }
    }
}
