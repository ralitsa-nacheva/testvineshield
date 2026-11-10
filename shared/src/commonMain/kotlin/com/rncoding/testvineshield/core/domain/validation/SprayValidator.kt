package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.SprayRecord
import com.rncoding.testvineshield.core.domain.error.ActivityError
import com.rncoding.testvineshield.core.domain.time.VineyardTimeZoneCatalog

class SprayValidator(private val zones: VineyardTimeZoneCatalog = VineyardTimeZoneCatalog()) {
    fun validate(record: SprayRecord): ActivityError.InvalidInput? {
        fun invalid(message: String) = ActivityError.InvalidInput(message)
        if (record.activityId <= 0 || record.revision < 0) return invalid("Invalid spray record identity.")
        if ((record.revision == 0L && record.generation != null) || (record.revision > 0 && (record.generation == null || !Regex("[0-9a-f]{32}").matches(record.generation)))) return invalid("Invalid saved spray version. Reload the record.")
        if (!zones.contains(record.timeZone)) return invalid("Choose a supported time zone.")
        if (record.localTime != null && !Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(record.localTime))
            return invalid("Enter local time as HH:mm, or leave it unknown.")
        fun positive(value: Double?) = value == null || (value.isFinite() && value > 0.0)
        if (!positive(record.treatedArea) || !positive(record.waterQuantity)) return invalid("Known area and water quantity must be finite and greater than zero.")
        if (record.treatedArea != null && record.areaUnit == null) return invalid("Choose the treated-area unit.")
        if (record.waterQuantity != null && record.waterUnit == null) return invalid("Choose the carrier-water unit.")
        if (record.waterUnit?.volume == false) return invalid("Carrier water requires a volume unit.")
        if ((record.notes?.length ?: 0) > 4000) return invalid("Notes must be at most 4000 characters.")
        if (record.products.isEmpty() || record.products.size > 20) return invalid("Record between 1 and 20 product lines.")
        val knownIds = record.products.map { it.lineId }.filter { it > 0 }
        if (knownIds.distinct().size != knownIds.size) return invalid("Duplicate product-line identity.")
        for ((index, line) in record.products.withIndex()) {
            if (line.lineId < 0 || line.productName.isBlank() || line.productName.length > 200)
                return invalid("Product ${index + 1}: enter a name of 1–200 characters.")
            if (!positive(line.quantity)) return invalid("Product ${index + 1}: a known quantity must be finite and greater than zero.")
            if (line.quantity != null && line.unit == null) return invalid("Product ${index + 1}: choose the quantity unit.")
            val rate = line.rate
            if (rate != null && (!rate.quantity.isFinite() || rate.quantity <= 0.0 || !rate.basisQuantity.isFinite() || rate.basisQuantity <= 0.0))
                return invalid("Product ${index + 1}: rate product and basis amounts must be finite and greater than zero.")
            if ((line.labelReference?.length ?: 0) > 500) return invalid("Product ${index + 1}: label reference must be at most 500 characters.")
        }
        return null
    }
}
