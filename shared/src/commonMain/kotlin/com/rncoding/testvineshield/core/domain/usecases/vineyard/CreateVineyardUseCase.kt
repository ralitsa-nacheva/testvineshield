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
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.validation.VineyardValidator
import kotlinx.datetime.TimeZone

class CreateVineyardUseCase(
    private val repository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val validator: VineyardValidator,
    private val clock: AppClock
) {

    suspend operator fun invoke(
        input: VineyardInput
    ): Result<Long, AppError> {

        val user =
            authenticatedUserProvider.currentUserOrNull()
                ?: return Result.Error(
                    AuthError.NoActiveSession
                )

        val normalizedInput =
            input.copy(
                name = input.name.trim(),
                country = input.country.trim(),
                city = input.city.trim(),
                timeZone = input.timeZone.trim()
            )

        val validationError =
            validator.validate(normalizedInput)

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

        val sortOrderResult =
            repository.getNextSortOrder(
                userId = user.userId
            )

        val sortOrder =
            when (sortOrderResult) {

                is Result.Success<*> ->
                    sortOrderResult.data as Int

                is Result.Error<*> ->
                    return Result.Error(
                        sortOrderResult.error
                    )

                else ->
                    return Result.Error(
                        VineyardValidationError(
                            field = VineyardField.NAME,
                            userMessage = "Unable to create the vineyard."
                        )
                    )
            }

        val today =
            clock.today(
                normalizedInput.timeZone
            )

        return repository.createVineyard(
            VineyardDomainModel(
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
        )
    }
}