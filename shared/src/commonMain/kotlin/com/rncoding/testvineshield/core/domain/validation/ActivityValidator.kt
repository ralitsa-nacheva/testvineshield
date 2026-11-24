
package com.rncoding.testvineshield.core.domain.validation

import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.error.ActivityValidationReason
import com.rncoding.testvineshield.core.domain.error.ActivityError

class ActivityValidator {

    fun validate(
        activity: ActivityDomainModel
    ): ActivityError.InvalidInput? {

        if (activity.vineyardId <= 0L) {
            return ActivityError.InvalidInput("Select a valid vineyard.", reason = ActivityValidationReason.Vineyard)
        }

        if (activity.blockId != null && activity.blockId <= 0L) {
            return ActivityError.InvalidInput("Select a valid block.", reason = ActivityValidationReason.Block)
        }

        if (activity.activityType.isBlank()) {
            return ActivityError.InvalidInput("Activity type is required.", reason = ActivityValidationReason.Type)
        }

        if (activity.season.isBlank()) {
            return ActivityError.InvalidInput("Season is required.", reason = ActivityValidationReason.Season)
        }

        if (ActivityStatus.entries.none {
                it.dbValue == activity.status
            }) {
            return ActivityError.InvalidInput("Select a valid activity status.", reason = ActivityValidationReason.Status)
        }

        if (ActivityPriority.entries.none {
                it.dbValue == activity.priority
            }) {
            return ActivityError.InvalidInput("Select a valid activity priority.", reason = ActivityValidationReason.Priority)
        }

        if (activity.phenologicalStage != null && PhenologicalStage.entries.none {
                it.dbValue == activity.phenologicalStage
            }) {
            return ActivityError.InvalidInput("Select a valid phenological stage.", reason = ActivityValidationReason.Stage)
        }

        // A plan is a target, not a start date. Early completion is valid;
        // keep plannedAt and completedAt as independent calendar dates.

        if (
            activity.status == ActivityStatus.COMPLETED.dbValue &&
            activity.completedAt == null
        ) {
            return ActivityError.InvalidInput("A completed activity requires a completion date.", reason = ActivityValidationReason.CompletionRequired)
        }

        if (
            activity.status != ActivityStatus.COMPLETED.dbValue &&
            activity.completedAt != null
        ) {
            return ActivityError.InvalidInput("Only completed activities may have a completion date.", reason = ActivityValidationReason.CompletionForbidden)
        }

        if (
            activity.treatmentProductQuantity != null &&
            (!activity.treatmentProductQuantity.isFinite() ||
                    activity.treatmentProductQuantity < 0.0)
        ) {
            return ActivityError.InvalidInput("Treatment quantity must be a valid non-negative number.", reason = ActivityValidationReason.TreatmentQuantity)
        }

        if (
            activity.waterQuantity != null &&
            (!activity.waterQuantity.isFinite() ||
                    activity.waterQuantity < 0.0)
        ) {
            return ActivityError.InvalidInput("Water quantity must be a valid non-negative number.", reason = ActivityValidationReason.WaterQuantity)
        }

        if (
            activity.treatmentProductRate != null &&
            (!activity.treatmentProductRate.isFinite() ||
                    activity.treatmentProductRate < 0.0)
        ) {
            return ActivityError.InvalidInput("Treatment rate must be a valid non-negative number.", reason = ActivityValidationReason.TreatmentRate)
        }

        return null
    }
}
