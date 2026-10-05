package com.rncoding.testvineshield.core.domain.datamodels

import androidx.room.ColumnInfo
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

data class BlockDomainModel(
    val blockId: Long,
    val vineyardId: Long, // foreign key
    val name: String,
    val area: Int,
    val vineVariety: String,
    val color: String,
    val rootStock: String,
    val rows: String,
    val rowSpacing: Int,
    val plantedAt: LocalDate,
    val phenologicalStage: PhenologicalStage?
)
