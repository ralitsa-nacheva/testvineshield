package com.rncoding.testvineshield.core.domain.datamodels.units

/** Stable persistence codes, not enum ordinals or translated labels. Null means unspecified. */
enum class AreaUnit(val code: String, val symbol: String) {
    SQUARE_METRE("m2", "m²"), HECTARE("ha", "ha"), ACRE("acre", "acre");
    companion object {
        fun fromCode(code: String): AreaUnit = entries.singleOrNull { it.code == code }
            ?: throw IllegalArgumentException("Unsupported saved area unit.")
    }
}

enum class LengthUnit(val code: String, val symbol: String) {
    METRE("m", "m"), CENTIMETRE("cm", "cm"), FOOT("ft", "ft");
    companion object {
        fun fromCode(code: String): LengthUnit = entries.singleOrNull { it.code == code }
            ?: throw IllegalArgumentException("Unsupported saved length unit.")
    }
}
