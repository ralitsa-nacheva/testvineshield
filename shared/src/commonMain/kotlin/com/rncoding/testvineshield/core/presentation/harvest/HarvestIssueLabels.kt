package com.rncoding.testvineshield.core.presentation.harvest

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

enum class HarvestDraftIssue { Season, Date, Decimal, Required, Positive, Brix, Unit }
sealed interface HarvestIssue {
    data class Failure(val error: AppError) : HarvestIssue
    data object ReviewFields : HarvestIssue
    data object InvalidScope : HarvestIssue
    data object DocumentSaveFailed : HarvestIssue
}

@Composable
internal fun localizedHarvestDraftIssue(issue: HarvestDraftIssue): String = stringResource(when (issue) {
    HarvestDraftIssue.Season -> Res.string.harvest_draft_season
    HarvestDraftIssue.Date -> Res.string.harvest_draft_date
    HarvestDraftIssue.Decimal -> Res.string.harvest_draft_decimal
    HarvestDraftIssue.Required -> Res.string.harvest_draft_required
    HarvestDraftIssue.Positive -> Res.string.harvest_draft_positive
    HarvestDraftIssue.Brix -> Res.string.harvest_draft_brix
    HarvestDraftIssue.Unit -> Res.string.harvest_draft_unit
})

@Composable
internal fun localizedHarvestIssue(issue: HarvestIssue): String = when (issue) {
    is HarvestIssue.Failure -> localizedAppErrorMessage(issue.error)
    HarvestIssue.ReviewFields -> stringResource(Res.string.harvest_issue_reviewfields)
    HarvestIssue.InvalidScope -> stringResource(Res.string.harvest_issue_invalidscope)
    HarvestIssue.DocumentSaveFailed -> stringResource(Res.string.harvest_issue_documentsavefailed)
}
