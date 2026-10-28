
package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes

class DiseaseValidator {

    private val reservedCodes = setOf(
        DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW,
        DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW,
        DiseaseCatalogueCodes.GRAPE_BOTRYTIS
    )

    fun validate(
        disease: DiseaseDomainModel
    ): DiseaseError.InvalidInput? {

        if (disease.name.isBlank()) {
            return DiseaseError.InvalidInput(
                "Disease name is required."
            )
        }

        if (disease.code != null &&
            disease.code.isBlank()
        ) {
            return DiseaseError.InvalidInput(
                "Disease code cannot be blank when provided."
            )
        }

        // Both custom-disease use cases trim codes before persistence.
        // Reserve these identifiers even before the built-in rows are seeded.
        if (disease.code?.trim() in reservedCodes) {
            return DiseaseError.InvalidInput(
                "This disease code is reserved for the predefined catalogue."
            )
        }

        return null
    }
}
