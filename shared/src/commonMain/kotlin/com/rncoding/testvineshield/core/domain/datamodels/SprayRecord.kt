package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import kotlinx.datetime.LocalDate

enum class SprayQuantityUnit(val code: String, val label: String, val volume: Boolean) {
    GRAM("g", "g", false), KILOGRAM("kg", "kg", false),
    MILLILITRE("ml", "mL", true), LITRE("l", "L", true), CUBIC_METRE("m3", "m³", true),
    US_GALLON("us_gal", "US gal", true), IMPERIAL_GALLON("imp_gal", "Imperial gal", true);
    companion object {
        fun fromCode(code: String): SprayQuantityUnit = entries.singleOrNull { it.code == code }
            ?: throw IllegalArgumentException("Unsupported spray quantity unit.")
    }
}

data class SprayProductLine(
    val lineId: Long = 0,
    val productName: String,
    val quantity: Double? = null,
    val unit: SprayQuantityUnit? = null,
    val labelReference: String? = null
)

/** Local calendar date/time with declared zone, not a fabricated UTC instant. */
data class SprayRecord(
    val activityId: Long,
    val applicationDate: LocalDate,
    val localTime: String?,
    val timeZone: String,
    val treatedArea: Double?,
    val areaUnit: AreaUnit?,
    val waterQuantity: Double?,
    val waterUnit: SprayQuantityUnit?,
    val notes: String?,
    val revision: Long = 0,
    val products: List<SprayProductLine>,
    val generation: String? = null
)

enum class SprayWriteOutcome { SAVED, CONFLICT, ACTIVITY_NOT_FOUND, INVALID_LINE_ID }
