
package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.HarvestEntity
import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel

class HarvestMapper {

    fun toDomain(
        entity: HarvestEntity
    ): HarvestDomainModel {
        return HarvestDomainModel(
            harvestId = entity.harvestId,
            blockId = entity.blockId,
            season = entity.season,
            harvestDate = entity.harvestDate,
            units = entity.units,
            quantity = entity.quantity,
            brixSugarContent = entity.brixSugarContent
        )
    }

    fun toEntity(
        domain: HarvestDomainModel
    ): HarvestEntity {
        return HarvestEntity(
            harvestId = domain.harvestId,
            blockId = domain.blockId,
            season = domain.season,
            harvestDate = domain.harvestDate,
            units = domain.units,
            quantity = domain.quantity,
            brixSugarContent = domain.brixSugarContent
        )
    }
}
