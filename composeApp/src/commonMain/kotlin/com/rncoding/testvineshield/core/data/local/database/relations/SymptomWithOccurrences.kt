package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity

data class SymptomWithOccurrences(
    @Embedded val symptom: SymptomEntity,
    @Relation(
       parentColumn = "symptom_id",
       entityColumn = "occurrence_id",
       associateBy = Junction(OccurrenceSymptomCrossRef::class)
   ) val occurrences: List<DiseaseOccurrenceEntity>
)
