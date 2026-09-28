package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseAlertEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseEntity

data class DiseaseWithAlerts(
    @Embedded val disease: DiseaseEntity,
    @Relation(
        parentColumn = "disease_id",
        entityColumn = "disease_id"
    )
    val diseaseAlerts: List<DiseaseAlertEntity>
)
