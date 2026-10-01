package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.VineyardInput
import com.rncoding.testvineshield.core.domain.error.VineyardField
import com.rncoding.testvineshield.core.domain.error.VineyardValidationError

class VineyardValidator {

    fun validate(
        input: VineyardInput
    ): VineyardValidationError? {

        val name = input.name.trim()
        val country = input.country.trim()
        val city = input.city.trim()
        val timeZone = input.timeZone.trim()

        if (name.isEmpty()) {
            return VineyardValidationError(
                field = VineyardField.NAME,
                userMessage = "Vineyard name is required."
            )
        }

        if (name.length > 100) {
            return VineyardValidationError(
                field = VineyardField.NAME,
                userMessage = "Vineyard name must be 100 characters or fewer."
            )
        }

        if (input.size <= 0.0) {
            return VineyardValidationError(
                field = VineyardField.SIZE,
                userMessage = "Vineyard size must be greater than zero."
            )
        }

        if (!input.size.isFinite()) {
            return VineyardValidationError(
                field = VineyardField.SIZE,
                userMessage = "Enter a valid vineyard size."
            )
        }

        if (country.isEmpty()) {
            return VineyardValidationError(
                field = VineyardField.COUNTRY,
                userMessage = "Country is required."
            )
        }

        if (city.isEmpty()) {
            return VineyardValidationError(
                field = VineyardField.CITY,
                userMessage = "City is required."
            )
        }

        if (!input.latitude.isFinite() ||
            input.latitude !in -90.0..90.0
        ) {
            return VineyardValidationError(
                field = VineyardField.LATITUDE,
                userMessage = "Latitude must be between -90 and 90."
            )
        }

        if (!input.longitude.isFinite() ||
            input.longitude !in -180.0..180.0
        ) {
            return VineyardValidationError(
                field = VineyardField.LONGITUDE,
                userMessage = "Longitude must be between -180 and 180."
            )
        }

        if (timeZone.isEmpty()) {
            return VineyardValidationError(
                field = VineyardField.TIME_ZONE,
                userMessage = "Time zone is required."
            )
        }

        if (input.elevation < -500 || input.elevation > 9000) {
            return VineyardValidationError(
                field = VineyardField.ELEVATION,
                userMessage = "Enter a valid elevation."
            )
        }

        return null
    }
}