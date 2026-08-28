package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity

data class VineyardWithActivities(
    @Embedded val vineyard: VineyardEntity,
    @Relation(
        parentColumn = "vineyard_id",
        entityColumn = "vineyard_id"
    )
    val activities: List<ActivityEntity>
)
