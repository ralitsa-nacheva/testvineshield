package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceActivityCrossRef


data class OccurrenceWithActivities(
    @Embedded
    val occurrence: DiseaseOccurrenceEntity,

    @Relation(
        parentColumn = "occurrence_id",
        entityColumn = "activity_id",
        associateBy = Junction(
            OccurrenceActivityCrossRef::class,
            parentColumn = "occurrence_id",
            entityColumn = "activity_id"
        )
    )
    val activities: List<ActivityEntity>
)