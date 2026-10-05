package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel

class BlockMapper {

    fun toDomain(
        entity: BlockEntity
    ): BlockDomainModel {
        return BlockDomainModel(
            blockId = entity.blockId,
            vineyardId = entity.vineyardId,
            name = entity.name,
            area = entity.area,
            vineVariety = entity.vineVariety,
            color = entity.color,
            rootStock = entity.rootStock,
            rows = entity.rows,
            rowSpacing = entity.rowSpacing,
            plantedAt = entity.plantedAt,
            phenologicalStage = entity.phenologicalStage
        )
    }

    fun toEntity(
        domain: BlockDomainModel
    ): BlockEntity {
        return BlockEntity(
            blockId = domain.blockId,
            vineyardId = domain.vineyardId,
            name = domain.name,
            area = domain.area,
            vineVariety = domain.vineVariety,
            color = domain.color,
            rootStock = domain.rootStock,
            rows = domain.rows,
            rowSpacing = domain.rowSpacing,
            plantedAt = domain.plantedAt,
            phenologicalStage = domain.phenologicalStage
        )
    }
}