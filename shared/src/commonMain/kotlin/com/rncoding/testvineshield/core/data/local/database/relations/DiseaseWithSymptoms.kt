package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseSymptomCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.SymptomEntity

data class DiseaseWithSymptoms(
    @Embedded val disease: DiseaseEntity,
    @Relation(
        parentColumn = "disease_id",
        entityColumn = "symptom_id",
        associateBy = Junction(DiseaseSymptomCrossRef::class)
    ) val symptoms: List<SymptomEntity>
)
