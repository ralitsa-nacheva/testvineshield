package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.ActivityError
import com.rncoding.testvineshield.core.domain.error.ActivityValidationReason
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedActivityError(error: ActivityError): String = when (error) {
    ActivityError.InvalidBlock -> stringResource(Res.string.activity_error_invalidblock)
    ActivityError.NotFound -> stringResource(Res.string.activity_error_notfound)
    ActivityError.LinkedToDisease -> stringResource(Res.string.activity_error_linkedtodisease)
    is ActivityError.InvalidInput -> {
        val resource = when (error.reason) {
            ActivityValidationReason.Vineyard -> Res.string.activity_validation_vineyard
            ActivityValidationReason.Block -> Res.string.activity_validation_block
            ActivityValidationReason.Type -> Res.string.activity_validation_type
            ActivityValidationReason.Season -> Res.string.activity_validation_season
            ActivityValidationReason.Status -> Res.string.activity_validation_status
            ActivityValidationReason.Priority -> Res.string.activity_validation_priority
            ActivityValidationReason.Stage -> Res.string.activity_validation_stage
            ActivityValidationReason.CompletionRequired -> Res.string.activity_validation_completionrequired
            ActivityValidationReason.CompletionForbidden -> Res.string.activity_validation_completionforbidden
            ActivityValidationReason.TreatmentQuantity -> Res.string.activity_validation_treatmentquantity
            ActivityValidationReason.WaterQuantity -> Res.string.activity_validation_waterquantity
            ActivityValidationReason.TreatmentRate -> Res.string.activity_validation_treatmentrate
            ActivityValidationReason.NewIdentity -> Res.string.activity_validation_newidentity
            ActivityValidationReason.InvalidIdentity -> Res.string.activity_validation_invalididentity
            ActivityValidationReason.InvalidScope -> Res.string.activity_validation_invalidscope
            null -> null
        }
        if (resource == null) stringResource(Res.string.error_review_input) else stringResource(resource)
    }
}
