package com.rncoding.testvineshield.core.domain.datamodels.enums

enum class ActivityStatus(
    val dbValue: String
) {
    PLANNED("planned"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    companion object {
        fun fromDbValue(
            value: String
        ): ActivityStatus =
            entries.firstOrNull {
                it.dbValue == value
            } ?: throw IllegalArgumentException(
                "Unknown ActivityStatus database value: $value"
            )
    }
}