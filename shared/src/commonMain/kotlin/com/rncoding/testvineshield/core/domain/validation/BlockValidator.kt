
package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.error.BlockError

class BlockValidator {

    fun validate(
        block: BlockDomainModel
    ): BlockError.InvalidInput? {

        if (block.vineyardId <= 0L) {
            return BlockError.InvalidInput(
                "Select a valid vineyard."
            )
        }

        if (block.name.isBlank()) {
            return BlockError.InvalidInput(
                "Block name is required."
            )
        }

        if (block.vineVariety.isBlank()) {
            return BlockError.InvalidInput(
                "Vine variety is required."
            )
        }

        if (block.area <= 0) {
            return BlockError.InvalidInput(
                "Block area must be greater than zero."
            )
        }

        if (block.rowCount <= 0) {
            return BlockError.InvalidInput(
                "Row count must be greater than zero."
            )
        }

        if (block.rowSpacing <= 0) {
            return BlockError.InvalidInput(
                "Row spacing must be greater than zero."
            )
        }

        return null
    }
}
