
package com.rncoding.testvineshield.core.domain.usecases.disease

import com.rncoding.testvineshield.core.domain.auth.AuthenticatedUserProvider
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.DiseaseRepository
import kotlinx.coroutines.CancellationException

class DeleteDiseaseUseCase(
    private val diseaseRepository: DiseaseRepository,
    private val authenticatedUserProvider: AuthenticatedUserProvider
) {

    suspend operator fun invoke(
        diseaseId: Long
    ): Result<Unit, AppError> {

        return try {
            if (authenticatedUserProvider.currentUserOrNull() == null) {
                return Result.Error(AuthError.NoActiveSession)
            }

            if (diseaseId <= 0L) {
                return Result.Error(
                    DiseaseError.InvalidInput(
                        "A valid disease ID is required."
                    )
                )
            }

            val existingDisease = diseaseRepository.getDisease(
                diseaseId = diseaseId
            ) ?: return Result.Error(DiseaseError.NotFound)

            if (existingDisease.isPredefined) {
                return Result.Error(
                    DiseaseError.PredefinedDisease
                )
            }

            if (diseaseRepository.isDiseaseReferenced(diseaseId)) {
                return Result.Error(
                    DiseaseError.DiseaseInUse
                )
            }

            val deleted = diseaseRepository.deleteDisease(diseaseId)

            if (deleted) {
                Result.Success(Unit)
            } else {
                // The record may have been deleted or become
                // referenced between the checks and deletion.
                val currentDisease = diseaseRepository.getDisease(diseaseId)

                when {
                    currentDisease == null ->
                        Result.Error(DiseaseError.NotFound)

                    currentDisease.isPredefined ->
                        Result.Error(DiseaseError.PredefinedDisease)

                    diseaseRepository.isDiseaseReferenced(diseaseId) ->
                        Result.Error(DiseaseError.DiseaseInUse)

                    else ->
                        Result.Error(
                            UnexpectedError(
                                IllegalStateException(
                                    "Disease deletion failed unexpectedly."
                                )
                            )
                        )
                }
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(UnexpectedError(e))
        }
    }
}
