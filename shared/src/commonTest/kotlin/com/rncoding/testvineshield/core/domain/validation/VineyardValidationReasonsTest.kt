package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.VineyardInput
import com.rncoding.testvineshield.core.domain.error.VineyardField
import com.rncoding.testvineshield.core.domain.error.VineyardValidationReason
import kotlin.test.*

class VineyardValidationReasonsTest {
    private val valid = VineyardInput("Test", 1.0, "BG", "Plovdiv", 42.1, 24.7, "Europe/Sofia", null)
    @Test fun failedRulesRetainFieldAndStableReasonWithoutChangingValidInput() {
        val cases = listOf(
            Triple(valid.copy(name = ""), VineyardField.NAME, VineyardValidationReason.NameRequired),
            Triple(valid.copy(name = "x".repeat(101)), VineyardField.NAME, VineyardValidationReason.NameTooLong),
            Triple(valid.copy(size = 0.0), VineyardField.SIZE, VineyardValidationReason.SizePositive),
            Triple(valid.copy(size = Double.NaN), VineyardField.SIZE, VineyardValidationReason.SizeNumber),
            Triple(valid.copy(country = ""), VineyardField.COUNTRY, VineyardValidationReason.CountryRequired),
            Triple(valid.copy(city = ""), VineyardField.CITY, VineyardValidationReason.CityRequired),
            Triple(valid.copy(latitude = 91.0), VineyardField.LATITUDE, VineyardValidationReason.LatitudeRange),
            Triple(valid.copy(longitude = -181.0), VineyardField.LONGITUDE, VineyardValidationReason.LongitudeRange),
            Triple(valid.copy(timeZone = ""), VineyardField.TIME_ZONE, VineyardValidationReason.TimeZoneRequired),
            Triple(valid.copy(elevation = 9001.0), VineyardField.ELEVATION, VineyardValidationReason.ElevationRange)
        )
        val validator = VineyardValidator()
        assertNull(validator.validate(valid))
        for ((input, field, reason) in cases) {
            val error = assertNotNull(validator.validate(input))
            assertEquals(field, error.field)
            assertEquals(reason, error.reason)
        }
    }
}
