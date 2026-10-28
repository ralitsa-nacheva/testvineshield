
package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.DiseaseDomainModel
import com.rncoding.testvineshield.core.domain.error.DiseaseError
import org.junit.Test
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class DiseaseValidatorTest {

    private val validator = DiseaseValidator()

    private fun validDisease() = DiseaseDomainModel(
        diseaseId = 0L,
        code = "CUSTOM-01",
        name = "Custom grapevine disease",
        description = "Test description",
        vectors = "Test vectors",
        factors = "Test factors",
        treatmentSuggestion = "Test treatment",
        isPredefined = false
    )

    @Test
    fun validDiseaseReturnsNoError() {
        val result = validator.validate(validDisease())

        assertNull(result)
    }

    @Test
    fun blankNameReturnsInvalidInput() {
        val result = validator.validate(
            validDisease().copy(name = "")
        )

        assertIs<DiseaseError.InvalidInput>(result)
        assertEquals("Disease name is required.", result.userMessage)
    }

    @Test
    fun whitespaceOnlyNameReturnsInvalidInput() {
        val result = validator.validate(
            validDisease().copy(name = "   \t  ")
        )

        assertIs<DiseaseError.InvalidInput>(result)
        assertEquals("Disease name is required.", result.userMessage)
    }

    @Test
    fun nullCodeIsAllowed() {
        val result = validator.validate(
            validDisease().copy(code = null)
        )

        assertNull(result)
    }

    @Test
    fun emptyCodeReturnsInvalidInput() {
        val result = validator.validate(
            validDisease().copy(code = "")
        )

        assertIs<DiseaseError.InvalidInput>(result)
        assertEquals(
            "Disease code cannot be blank when provided.",
            result.userMessage
        )
    }

    @Test
    fun whitespaceOnlyCodeReturnsInvalidInput() {
        val result = validator.validate(
            validDisease().copy(code = "   ")
        )

        assertIs<DiseaseError.InvalidInput>(result)
        assertEquals(
            "Disease code cannot be blank when provided.",
            result.userMessage
        )
    }

    @Test
    fun optionalDescriptionFieldsMayBeEmpty() {
        val result = validator.validate(
            validDisease().copy(
                description = "",
                vectors = "",
                factors = "",
                treatmentSuggestion = ""
            )
        )

        assertNull(result)
    }

    @Test
    fun leadingAndTrailingWhitespaceInNameIsAllowed() {
        val result = validator.validate(
            validDisease().copy(
                name = "  Downy mildew  "
            )
        )

        assertNull(result)
    }

    @Test
    fun positiveDiseaseIdIsNotValidatedHere() {
        val result = validator.validate(
            validDisease().copy(diseaseId = 10L)
        )

        assertNull(result)
    }

    @Test
    fun predefinedFlagIsNotValidatedHere() {
        val result = validator.validate(
            validDisease().copy(isPredefined = true)
        )

        assertNull(result)
    }

    @Test
    fun reservedCodesAreRejectedIncludingSurroundingWhitespace() {
        val codes = listOf(
            DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW,
            DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW,
            DiseaseCatalogueCodes.GRAPE_BOTRYTIS
        )
        for (code in codes) {
            for (input in listOf(code, "  $code  ")) {
                assertIs<DiseaseError.InvalidInput>(
                    validator.validate(validDisease().copy(code = input))
                )
            }
        }
    }

    @Test
    fun predefinedFlagCannotBypassReservedCodeValidation() {
        assertIs<DiseaseError.InvalidInput>(
            validator.validate(
                validDisease().copy(
                    code = DiseaseCatalogueCodes.GRAPE_BOTRYTIS,
                    isPredefined = true
                )
            )
        )
    }
}
