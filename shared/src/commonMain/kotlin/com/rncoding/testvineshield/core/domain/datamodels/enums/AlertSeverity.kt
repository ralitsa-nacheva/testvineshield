package com.rncoding.testvineshield.core.domain.datamodels.enums

enum class AlertSeverity(
    val dbValue: String
) {
    INFO("info"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical");

    companion object {
        fun fromDbValue(
            value: String
        ): AlertSeverity =
            entries.firstOrNull {
                it.dbValue == value
            } ?: throw IllegalArgumentException(
                "Unknown AlertSeverity database value: $value"
            )
    }
}