package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.ActivityError
import com.rncoding.testvineshield.core.domain.error.SprayValidationReason
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedSprayValidationError(error: ActivityError.InvalidInput): String {
    val reason = error.sprayReason ?: return error.userMessage
    val resource = when (reason) {
        SprayValidationReason.RecordIdentity -> Res.string.spray_error_recordidentity
        SprayValidationReason.Version -> Res.string.spray_error_version
        SprayValidationReason.Zone -> Res.string.spray_error_zone
        SprayValidationReason.Time -> Res.string.spray_error_time
        SprayValidationReason.Positive -> Res.string.spray_error_positive
        SprayValidationReason.AreaUnit -> Res.string.spray_error_areaunit
        SprayValidationReason.WaterUnit -> Res.string.spray_error_waterunit
        SprayValidationReason.WaterVolume -> Res.string.spray_error_watervolume
        SprayValidationReason.Notes -> Res.string.spray_error_notes
        SprayValidationReason.LineCount -> Res.string.spray_error_linecount
        SprayValidationReason.DuplicateLine -> Res.string.spray_error_duplicateline
        SprayValidationReason.ProductName -> Res.string.spray_error_productname
        SprayValidationReason.ProductQuantity -> Res.string.spray_error_productquantity
        SprayValidationReason.ProductUnit -> Res.string.spray_error_productunit
        SprayValidationReason.ProductRate -> Res.string.spray_error_productrate
        SprayValidationReason.ProductReference -> Res.string.spray_error_productreference
        SprayValidationReason.Conflict -> Res.string.spray_error_conflict
        SprayValidationReason.LineIdentity -> Res.string.spray_error_lineidentity
        SprayValidationReason.ActivityIdentity -> Res.string.spray_error_activityidentity
        SprayValidationReason.DeleteReload -> Res.string.spray_error_deletereload
        SprayValidationReason.ExportSaveFirst -> Res.string.spray_error_exportsavefirst
        SprayValidationReason.ExportReload -> Res.string.spray_error_exportreload
        SprayValidationReason.ExportTooLarge -> Res.string.spray_error_exporttoolarge
    }
    return when (reason) {
        SprayValidationReason.ProductName, SprayValidationReason.ProductQuantity,
        SprayValidationReason.ProductUnit, SprayValidationReason.ProductRate,
        SprayValidationReason.ProductReference -> {
            val number = error.productNumber
            if (number == null || number <= 0) error.userMessage else stringResource(resource, number)
        }
        else -> stringResource(resource)
    }
}
