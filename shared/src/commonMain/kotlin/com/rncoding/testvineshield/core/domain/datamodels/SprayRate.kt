package com.rncoding.testvineshield.core.domain.datamodels

enum class SprayRateBasis(val code: String, val label: String, val dimension: String) {
    SQUARE_METRE("area_m2", "m² treated area", "treated_area"), HECTARE("area_ha", "ha treated area", "treated_area"),
    ACRE("area_acre", "acre treated area", "treated_area"),
    MILLILITRE("water_ml", "mL carrier water", "carrier_water"), LITRE("water_l", "L carrier water", "carrier_water"),
    CUBIC_METRE("water_m3", "m³ carrier water", "carrier_water"),
    US_GALLON("water_us_gal", "US gal carrier water", "carrier_water"),
    IMPERIAL_GALLON("water_imp_gal", "Imperial gal carrier water", "carrier_water");
    companion object {
        fun fromCode(code: String): SprayRateBasis = entries.singleOrNull { it.code == code }
            ?: throw IllegalArgumentException("Unsupported saved rate basis.")
    }
}

/** Recorded product amount per explicit amount of treated area or carrier water. No calculation. */
data class SprayRate(val quantity: Double, val productUnit: SprayQuantityUnit, val basisQuantity: Double, val basis: SprayRateBasis)
