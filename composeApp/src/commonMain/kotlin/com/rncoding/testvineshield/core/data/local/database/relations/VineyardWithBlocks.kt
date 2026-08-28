package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity

data class VineyardWithBlocks(
    @Embedded val vineyard: VineyardEntity,
    @Relation(
        parentColumn = "vineyard_id",
        entityColumn = "vineyard_id"
    )
    val blocks: List<BlockEntity>
)