package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity

data class SymptomWithDiseases(
    @Embedded val symptom: SymptomEntity,
    @Relation(
        parentColumn = "symptom_id",
        entityColumn = "disease_id",
        associateBy = Junction(DiseaseSymptomCrossRef::class)
    ) val diseases: List<DiseaseEntity>
)
