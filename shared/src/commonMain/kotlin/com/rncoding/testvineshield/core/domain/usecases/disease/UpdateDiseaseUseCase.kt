
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

class UpdateDiseaseUseCase(
    private val diseaseRepository: DiseaseRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider,
    private val validator: DiseaseValidator
) {

    suspend operator fun invoke(
        disease: DiseaseDomainModel
    ): Result<Unit, AppError> {

        return try {
            if (authenticatedUserProvider.currentUserOrNull() == null) {
                return Result.Error(AuthError.NoActiveSession)
            }

            if (disease.diseaseId <= 0L) {
                return Result.Error(
                    DiseaseError.InvalidInput(
                        "A valid disease ID is required."
                    )
                )
            }

            val validationError = validator.validate(disease)

            if (validationError != null) {
                return Result.Error(validationError)
            }

            val existingDisease = diseaseRepository.getDisease(
                diseaseId = disease.diseaseId
            ) ?: return Result.Error(DiseaseError.NotFound)

            if (existingDisease.isPredefined) {
                return Result.Error(
                    DiseaseError.PredefinedDisease
                )
            }

            val normalizedDisease = disease.copy(
                code = disease.code?.trim(),
                name = disease.name.trim(),
                description = disease.description.trim(),
                vectors = disease.vectors.trim(),
                factors = disease.factors.trim(),
                treatmentSuggestion = disease.treatmentSuggestion.trim(),
                isPredefined = false
            )

            val updated = diseaseRepository.updateDisease(
                normalizedDisease
            )

            if (updated) {
                Result.Success(Unit)
            } else {
                Result.Error(DiseaseError.NotFound)
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
