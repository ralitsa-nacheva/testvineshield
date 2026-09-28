package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity

data class ActivityWithOccurrences(
    @Embedded
    val activity: ActivityEntity,

    @Relation(
        parentColumn = "activity_id",
        entityColumn = "occurrence_id",
        associateBy = Junction(
            ActivityOccurrenceCrossRef::class,
            parentColumn = "activity_id",
            entityColumn = "occurrence_id"
        )
    )
    val occurrences: List<DiseaseOccurrenceEntity>
)