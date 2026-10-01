package com.rncoding.testvineshield.core.data.local.database.converters

import androidx.room.TypeConverter
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertSeverity
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

class DomainEnumRoomConverters {

    @TypeConverter
    fun activityStatusToString(
        value: ActivityStatus
    ): String =
        value.dbValue

    @TypeConverter
    fun stringToActivityStatus(
        value: String
    ): ActivityStatus =
        ActivityStatus.fromDbValue(value)

    @TypeConverter
    fun activityPriorityToString(
        value: ActivityPriority
    ): String =
        value.dbValue

    @TypeConverter
    fun stringToActivityPriority(
        value: String
    ): ActivityPriority =
        ActivityPriority.fromDbValue(value)

    @TypeConverter
    fun phenologicalStageToString(
        value: PhenologicalStage
    ): String =
        value.dbValue

    @TypeConverter
    fun stringToPhenologicalStage(
        value: String
    ): PhenologicalStage =
        PhenologicalStage.fromDbValue(value)

    @TypeConverter
    fun diseaseOccurrenceStatusToString(
        value: DiseaseOccurrenceStatus
    ): String =
        value.dbValue

    @TypeConverter
    fun stringToDiseaseOccurrenceStatus(
        value: String
    ): DiseaseOccurrenceStatus =
        DiseaseOccurrenceStatus.fromDbValue(value)

    @TypeConverter
    fun alertStatusToString(
        value: AlertStatus
    ): String =
        value.dbValue

    @TypeConverter
    fun stringToAlertStatus(
        value: String
    ): AlertStatus =
        AlertStatus.fromDbValue(value)

    @TypeConverter
    fun alertSeverityToString(
        value: AlertSeverity
    ): String =
        value.dbValue

    @TypeConverter
    fun stringToAlertSeverity(
        value: String
    ): AlertSeverity =
        AlertSeverity.fromDbValue(value)
}