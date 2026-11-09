package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.error.BlockError

class BlockValidator {
    fun validate(block: BlockDomainModel): BlockError.InvalidInput? {
        if (block.vineyardId <= 0L) return BlockError.InvalidInput("Select a valid vineyard.")
        if (block.name.isBlank()) return BlockError.InvalidInput("Block name is required.")
        if (block.vineVariety.isBlank()) return BlockError.InvalidInput("Vine variety is required.")
        block.area?.let {
            if (!it.isFinite() || it <= 0.0) return BlockError.InvalidInput("Known block area must be finite and greater than zero.")
        }
        block.rowCount?.let {
            if (it <= 0) return BlockError.InvalidInput("Known row count must be greater than zero.")
        }
        block.rowSpacing?.let {
            if (!it.isFinite() || it <= 0.0) return BlockError.InvalidInput("Known row spacing must be finite and greater than zero.")
        }
        // Unknown is null. Neither a planting date nor a growth stage is inferred here.
        return null
    }
}
