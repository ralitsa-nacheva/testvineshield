
package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import com.rncoding.testvineshield.core.domain.validation.DiseaseValidator
import kotlinx.coroutines.CancellationException

class CreateDiseaseUseCase(
    private val diseaseRepository: DiseaseRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val validator: DiseaseValidator
) {

    suspend operator fun invoke(
        disease: DiseaseDomainModel
    ): Result<Long, AppError> {

        if (authenticatedUserProvider.currentUserOrNull() == null) {
            return Result.Error(AuthError.NoActiveSession)
        }

        if (disease.diseaseId != 0L) {
            return Result.Error(
                DiseaseError.InvalidInput(
                    "A new disease must not have an existing ID."
                )
            )
        }

        val validationError = validator.validate(disease)

        if (validationError != null) {
            return Result.Error(validationError)
        }

        return try {
            val normalizedDisease = disease.copy(
                diseaseId = 0L,
                code = disease.code?.trim(),
                name = disease.name.trim(),
                description = disease.description.trim(),
                vectors = disease.vectors.trim(),
                factors = disease.factors.trim(),
                treatmentSuggestion = disease.treatmentSuggestion.trim(),
                isPredefined = false
            )

            val newDiseaseId = diseaseRepository.createDisease(
                normalizedDisease
            )

            if (newDiseaseId > 0L) {
                Result.Success(newDiseaseId)
            } else {
                Result.Error(
                    UnexpectedError(
                        IllegalStateException(
                            "Disease insertion did not return a valid ID."
                        )
                    )
                )
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
