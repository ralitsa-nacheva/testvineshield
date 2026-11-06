package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.VineyardInput
import com.rncoding.testvineshield.core.domain.error.VineyardField
import kotlin.test.*

class VineyardElevationValidationTest {
    private val validator = VineyardValidator()
    private val input = VineyardInput("Vineyard", 1.25, "BG", "Plovdiv", 42.1, 24.7, "Europe/Sofia", 200.0)
    @Test fun unknownElevationIsAllowed() { assertNull(validator.validate(input.copy(elevation = null))) }
    @Test fun decimalAndNegativeElevationsAreAllowed() {
        for (value in listOf(123.45, -12.5, 0.0, -500.0, 9000.0)) assertNull(validator.validate(input.copy(elevation = value)))
    }
    @Test fun outOfRangeElevationIsRejected() {
        for (value in listOf(-500.01, 9000.01)) assertEquals(VineyardField.ELEVATION, validator.validate(input.copy(elevation = value))?.field)
    }
    @Test fun directDomainCallsCannotPersistNonFiniteElevation() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertEquals(VineyardField.ELEVATION, validator.validate(input.copy(elevation = value))?.field)
        }
    }
}
