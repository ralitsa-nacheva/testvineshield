package com.rncoding.testvineshield.core.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity

data class BlockWithActivities(
    @Embedded val block: BlockEntity,
    @Relation(
        parentColumn = "block_id",
        entityColumn = "block_id"
    )
    val activities: List<ActivityEntity>
)
