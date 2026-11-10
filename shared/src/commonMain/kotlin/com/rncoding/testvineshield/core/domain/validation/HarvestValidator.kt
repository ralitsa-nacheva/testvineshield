
package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.error.HarvestError

class HarvestValidator {

    fun validate(
        harvest: HarvestDomainModel
    ): HarvestError.InvalidInput? {

        if (harvest.blockId <= 0L) {
            return HarvestError.InvalidInput(
                "Select a valid block."
            )
        }

        if (harvest.season.isBlank()) {
            return HarvestError.InvalidInput(
                "Harvest season is required."
            )
        }

        if (harvest.units.isBlank()) {
            return HarvestError.InvalidInput(
                "Harvest units are required."
            )
        }

        if (!harvest.quantity.isFinite() || harvest.quantity <= 0.0) {
            return HarvestError.InvalidInput(
                "Harvest quantity must be a finite value greater than zero."
            )
        }

        val brix = harvest.brixSugarContent
        if (brix != null && (!brix.isFinite() || brix !in 0.0..100.0)) {
            return HarvestError.InvalidInput(
                "Brix sugar content must be a finite value between 0 and 100."
            )
        }

        return null
    }
}
