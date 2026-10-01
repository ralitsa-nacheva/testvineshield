package com.rncoding.testvineshield.core.domain.datamodels.enums

enum class ActivityPriority(
    val dbValue: String
) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    URGENT("urgent");

    companion object {
        fun fromDbValue(
            value: String
        ): ActivityPriority =
            entries.firstOrNull {
                it.dbValue == value
            } ?: throw IllegalArgumentException(
                "Unknown ActivityPriority database value: $value"
            )
    }
}