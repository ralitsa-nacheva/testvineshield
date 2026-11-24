package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedHarvestError(error: HarvestError): String = when (error) {
    HarvestError.NotFound -> stringResource(Res.string.harvest_error_not_found)
    is HarvestError.InvalidInput -> error.reason?.let { reason -> stringResource(when (reason) {
        HarvestValidationReason.Block -> Res.string.harvest_error_block
        HarvestValidationReason.Season -> Res.string.harvest_error_season
        HarvestValidationReason.Units -> Res.string.harvest_error_units
        HarvestValidationReason.Quantity -> Res.string.harvest_error_quantity
        HarvestValidationReason.Brix -> Res.string.harvest_error_brix
        HarvestValidationReason.Scope -> Res.string.harvest_error_scope
        HarvestValidationReason.Identity -> Res.string.harvest_error_identity
        HarvestValidationReason.NewIdentity -> Res.string.harvest_error_newidentity
        HarvestValidationReason.Harvest -> Res.string.harvest_error_harvest
        HarvestValidationReason.ExportScope -> Res.string.harvest_error_exportscope
        HarvestValidationReason.ExportEmpty -> Res.string.harvest_error_exportempty
        HarvestValidationReason.ExportReload -> Res.string.harvest_error_exportreload
        HarvestValidationReason.ExportTooLarge -> Res.string.harvest_error_exporttoolarge
    }) } ?: error.userMessage
}
