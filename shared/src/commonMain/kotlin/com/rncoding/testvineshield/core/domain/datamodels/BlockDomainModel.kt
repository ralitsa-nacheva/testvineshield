package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.units.LengthUnit
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
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
    val rowCount: Int,
    val rowSpacing: Int,
    val plantedAt: LocalDate,
    val phenologicalStage: PhenologicalStage?,
    val areaUnit: AreaUnit? = null,
    val rowSpacingUnit: LengthUnit? = null
)
