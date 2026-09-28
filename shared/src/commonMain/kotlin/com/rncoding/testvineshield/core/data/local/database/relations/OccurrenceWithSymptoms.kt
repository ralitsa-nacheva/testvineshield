package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity

data class OccurrenceWithSymptoms(
    @Embedded val occurrence: DiseaseOccurrenceEntity,
    @Relation(
        parentColumn = "occurrence_id",
        entityColumn = "symptom_id",
        associateBy = Junction(OccurrenceSymptomCrossRef::class)
    ) val symptoms: List<SymptomEntity>
)