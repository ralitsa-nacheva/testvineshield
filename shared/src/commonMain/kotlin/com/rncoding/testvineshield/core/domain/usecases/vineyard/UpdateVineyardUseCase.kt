package com.rncoding.testvineshield.core.domain.usecases.vineyard

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardInput
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.VineyardField
import com.rncoding.testvineshield.core.domain.error.VineyardValidationError
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.time.AppClock
import com.rncoding.testvineshield.core.domain.validation.VineyardValidator
import kotlinx.datetime.TimeZone



class UpdateVineyardUseCase(
    private val repository: VineyardRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val validator: VineyardValidator,
    private val clock: AppClock
) {

    suspend operator fun invoke(
        vineyardId: Long,
        input: VineyardInput
    ): Result<Unit, AppError> {

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

        val existingResult =
            repository.getVineyard(
                userId = user.userId,
                vineyardId = vineyardId
            )

        val existing =
            when (existingResult) {

                is Result.Success<*> -> {
                    existingResult.data as VineyardDomainModel?
                }

                is Result.Error<*> -> {
                    return Result.Error(
                        existingResult.error
                    )
                }

                else -> null
            }

        if (existing == null) {
            return Result.Error(
                DatabaseError.NotFound(
                    entity = "Vineyard"
                )
            )
        }

        val updated =
            existing.copy(
                name = normalizedInput.name,
                size = normalizedInput.size,
                country = normalizedInput.country,
                city = normalizedInput.city,
                latitude = normalizedInput.latitude,
                longitude = normalizedInput.longitude,
                timeZone = normalizedInput.timeZone,
                elevation = normalizedInput.elevation,
                updatedAt = clock.today(normalizedInput.timeZone)
            )

        return repository.updateVineyard(updated)
    }
}