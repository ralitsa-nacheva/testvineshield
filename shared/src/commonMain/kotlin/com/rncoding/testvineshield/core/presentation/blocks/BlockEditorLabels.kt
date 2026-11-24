package com.rncoding.testvineshield.core.presentation.blocks

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

enum class BlockDraftIssue { NameRequired, VarietyRequired, Decimal, Positive, Rows, PlantedDate, StageFirst, ObservationDate }

sealed interface BlockEditorIssue {
    data class Failure(val error: AppError) : BlockEditorIssue
    data object InvalidRequest : BlockEditorIssue
    data object ReviewFields : BlockEditorIssue
}

@Composable
internal fun localizedBlockDraftIssue(issue: BlockDraftIssue): String = stringResource(when (issue) {
    BlockDraftIssue.NameRequired -> Res.string.block_validation_namerequired
    BlockDraftIssue.VarietyRequired -> Res.string.block_validation_varietyrequired
    BlockDraftIssue.Decimal -> Res.string.block_validation_decimal
    BlockDraftIssue.Positive -> Res.string.block_validation_positive
    BlockDraftIssue.Rows -> Res.string.block_validation_rows
    BlockDraftIssue.PlantedDate -> Res.string.block_validation_planteddate
    BlockDraftIssue.StageFirst -> Res.string.block_validation_stagefirst
    BlockDraftIssue.ObservationDate -> Res.string.block_validation_observationdate
})

@Composable
internal fun localizedBlockEditorIssue(issue: BlockEditorIssue): String = when (issue) {
    is BlockEditorIssue.Failure -> localizedAppErrorMessage(issue.error)
    BlockEditorIssue.InvalidRequest -> stringResource(Res.string.block_editor_invalid_request)
    BlockEditorIssue.ReviewFields -> stringResource(Res.string.block_editor_review_fields)
}
