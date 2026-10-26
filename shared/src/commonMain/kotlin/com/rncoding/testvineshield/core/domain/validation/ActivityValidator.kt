
package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.error.ActivityError

class ActivityValidator {

    fun validate(
        activity: ActivityDomainModel
    ): ActivityError.InvalidInput? {

        if (activity.vineyardId <= 0L) {
            return ActivityError.InvalidInput(
                "Select a valid vineyard."
            )
        }

        if (activity.blockId != null && activity.blockId <= 0L) {
            return ActivityError.InvalidInput(
                "Select a valid block."
            )
        }

        if (activity.activityType.isBlank()) {
            return ActivityError.InvalidInput(
                "Activity type is required."
            )
        }

        if (activity.season.isBlank()) {
            return ActivityError.InvalidInput(
                "Season is required."
            )
        }

        if (ActivityStatus.entries.none {
                it.dbValue == activity.status
            }) {
            return ActivityError.InvalidInput(
                "Select a valid activity status."
            )
        }

        if (ActivityPriority.entries.none {
                it.dbValue == activity.priority
            }) {
            return ActivityError.InvalidInput(
                "Select a valid activity priority."
            )
        }

        if (PhenologicalStage.entries.none {
                it.dbValue == activity.phenologicalStage
            }) {
            return ActivityError.InvalidInput(
                "Select a valid phenological stage."
            )
        }

        if (
            activity.completedAt != null &&
            activity.completedAt < activity.plannedAt
        ) {
            return ActivityError.InvalidInput(
                "Completion date cannot precede the planned date."
            )
        }

        if (
            activity.status == ActivityStatus.COMPLETED.dbValue &&
            activity.completedAt == null
        ) {
            return ActivityError.InvalidInput(
                "A completed activity requires a completion date."
            )
        }

        if (
            activity.status != ActivityStatus.COMPLETED.dbValue &&
            activity.completedAt != null
        ) {
            return ActivityError.InvalidInput(
                "Only completed activities may have a completion date."
            )
        }

        if (
            activity.treatmentProductQuantity != null &&
            (!activity.treatmentProductQuantity.isFinite() ||
                    activity.treatmentProductQuantity < 0.0)
        ) {
            return ActivityError.InvalidInput(
                "Treatment quantity must be a valid non-negative number."
            )
        }

        if (
            activity.waterQuantity != null &&
            (!activity.waterQuantity.isFinite() ||
                    activity.waterQuantity < 0.0)
        ) {
            return ActivityError.InvalidInput(
                "Water quantity must be a valid non-negative number."
            )
        }

        if (
            activity.treatmentProductRate != null &&
            (!activity.treatmentProductRate.isFinite() ||
                    activity.treatmentProductRate < 0.0)
        ) {
            return ActivityError.InvalidInput(
                "Treatment rate must be a valid non-negative number."
            )
        }

        return null
    }
}
