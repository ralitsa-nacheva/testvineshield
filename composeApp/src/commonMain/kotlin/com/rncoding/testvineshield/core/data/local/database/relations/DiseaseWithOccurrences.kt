package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity

data class DiseaseWithOccurrences(
    @Embedded val disease: DiseaseEntity,
    @Relation(
        parentColumn = "disease_id",
        entityColumn = "occurrence_id" // is this correct
    )
    val diseaseOccurrence: List<DiseaseOccurrenceEntity>
    )
