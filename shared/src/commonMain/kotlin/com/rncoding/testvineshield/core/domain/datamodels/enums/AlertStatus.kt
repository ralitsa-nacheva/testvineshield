package com.rncoding.testvineshield.core.domain.datamodels.enums

enum class AlertStatus(
    val dbValue: String
) {
    ACTIVE("active"),
    ACKNOWLEDGED("acknowledged"),
    RESOLVED("resolved");

    companion object {
        fun fromDbValue(
            value: String
        ): AlertStatus =
            entries.firstOrNull {
                it.dbValue == value
            } ?: throw IllegalArgumentException(
                "Unknown AlertStatus database value: $value"
            )
    }
}