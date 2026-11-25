package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedBlockError(error: BlockError): String = when (error) {
    BlockError.NotFound -> stringResource(Res.string.block_error_not_found)
    is BlockError.InvalidInput -> stringResource(when (error.reason) {
        BlockValidationReason.Vineyard -> Res.string.block_domain_validation_vineyard
        BlockValidationReason.Name -> Res.string.block_domain_validation_name
        BlockValidationReason.Variety -> Res.string.block_domain_validation_variety
        BlockValidationReason.Area -> Res.string.block_domain_validation_area
        BlockValidationReason.Rows -> Res.string.block_domain_validation_rows
        BlockValidationReason.Spacing -> Res.string.block_domain_validation_spacing
        BlockValidationReason.Identity -> Res.string.block_domain_validation_identity
        BlockValidationReason.Scope -> Res.string.block_domain_validation_scope
        BlockValidationReason.NewIdentity -> Res.string.block_domain_validation_newidentity
        BlockValidationReason.VineyardIdentity -> Res.string.block_domain_validation_vineyardidentity
        null -> Res.string.error_review_input
    })
}
