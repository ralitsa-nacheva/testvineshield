
package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardInput
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.VineyardField
import com.rncoding.testvineshield.core.domain.error.VineyardValidationError
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.validation.VineyardValidator
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone

class CreateVineyardUseCase(
    private val repository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val validator: VineyardValidator,
    private val clock: AppClock,
    private val weatherHistoryRiskScheduler: WeatherHistoryRiskScheduler
) {

    suspend operator fun invoke(
        input: VineyardInput
    ): Result<Long, AppError> {

        val user = authenticatedUserProvider.currentUserOrNull()
            ?: return Result.Error(AuthError.NoActiveSession)

        val normalizedInput = input.copy(
            name = input.name.trim(),
            country = input.country.trim(),
            city = input.city.trim(),
            timeZone = input.timeZone.trim()
        )

        val validationError = validator.validate(normalizedInput)

        if (validationError != null) {
            return Result.Error(validationError)
        }

        try {
            TimeZone.of(normalizedInput.timeZone)
        } catch (e: IllegalArgumentException) {
            return Result.Error(
                VineyardValidationError(
                    field = VineyardField.TIME_ZONE,
                    userMessage = "Enter a valid time zone.",
                    cause = e
                )
            )
        }

        val sortOrder = when (
            val sortOrderResult = repository.getNextSortOrder(
                userId = user.userId
            )
        ) {
            is Result.Success -> sortOrderResult.data

            is Result.Error -> {
                return Result.Error(sortOrderResult.error)
            }
        }

        val today = clock.today(normalizedInput.timeZone)

        val vineyard = VineyardDomainModel(
            vineyardId = 0L,
            userId = user.userId,
            name = normalizedInput.name,
            size = normalizedInput.size,
            country = normalizedInput.country,
            city = normalizedInput.city,
            latitude = normalizedInput.latitude,
            longitude = normalizedInput.longitude,
            timeZone = normalizedInput.timeZone,
            elevation = normalizedInput.elevation,
            sortOrder = sortOrder,
            createdAt = today,
            updatedAt = today
        )

        val createResult = repository.createVineyard(vineyard)

        return when (createResult) {

            is Result.Error -> {
                Result.Error(createResult.error)
            }

            is Result.Success -> {
                val vineyardId = createResult.data

                try {
                    weatherHistoryRiskScheduler.schedule(
                        userId = user.userId,
                        vineyardId = vineyardId
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The vineyard was successfully saved.
                    // A background scheduling failure must not
                    // be reported as a vineyard creation failure.
                    //
                    // A later session reconciliation can retry
                    // scheduling this vineyard.
                }

                Result.Success(vineyardId)
            }
        }
    }
}
