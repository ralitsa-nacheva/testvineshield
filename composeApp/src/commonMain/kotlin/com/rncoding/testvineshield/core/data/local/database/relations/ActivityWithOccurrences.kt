package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceTreatmentCrossRef


data class ActivityWithOccurrences(
    @Embedded val activity: ActivityEntity,
    @Relation(
        parentColumn = "activity_id",
        entityColumn = "occurrence_id",
        associateBy = Junction(OccurrenceTreatmentCrossRef::class)
    ) val occurrences: List<DiseaseOccurrenceEntity>
)