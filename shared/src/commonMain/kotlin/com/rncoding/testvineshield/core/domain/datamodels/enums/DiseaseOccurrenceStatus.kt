package com.rncoding.testvineshield.core.domain.datamodels.enums

enum class DiseaseOccurrenceStatus(
    val dbValue: String
) {
    ACTIVE("active"),
    MONITORING("monitoring"),
    RESOLVED("resolved");

    companion object {
        fun fromDbValue(
            value: String
        ): DiseaseOccurrenceStatus =
            entries.firstOrNull {
                it.dbValue == value
            } ?: throw IllegalArgumentException(
                "Unknown DiseaseOccurrenceStatus database value: $value"
            )
    }
}