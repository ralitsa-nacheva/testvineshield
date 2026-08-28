package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseOccurrenceEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseTreatmentEntity
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceTreatmentCrossRef

data class OccurrenceWithTreatments(
    @Embedded val occurrence: DiseaseOccurrenceEntity,
    @Relation(
        parentColumn = "occurrence_id",
        entityColumn = "treatment_id",
        associateBy = Junction(OccurrenceTreatmentCrossRef::class)
    ) val treatments: List<DiseaseTreatmentEntity>
)