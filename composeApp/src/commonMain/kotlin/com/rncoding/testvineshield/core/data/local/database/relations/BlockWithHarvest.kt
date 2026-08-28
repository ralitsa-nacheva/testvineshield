package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.entities.HarvestEntity

data class BlockWithHarvest(
    @Embedded val block: BlockEntity,
    @Relation(
        parentColumn = "block_id",
        entityColumn = "block_id"
    )
    val harvest: List<HarvestEntity>
)
