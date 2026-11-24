package com.rncoding.testvineshield.core.presentation.spray

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

enum class SprayDraftIssue { Date, Time, Decimal, Positive, AreaUnit, WaterUnit, Name, QuantityUnit, RateAmount, BasisAmount, RateUnit, RateBasis }
sealed interface SprayIssue {
    data class Failure(val error: AppError) : SprayIssue
    data object ReviewFields : SprayIssue
    data object DraftConflict : SprayIssue
    data object DocumentSaveFailed : SprayIssue
}

@Composable
internal fun localizedSprayDraftIssue(issue: SprayDraftIssue): String = stringResource(when (issue) {
    SprayDraftIssue.Date -> Res.string.spray_draft_date
    SprayDraftIssue.Time -> Res.string.spray_draft_time
    SprayDraftIssue.Decimal -> Res.string.spray_draft_decimal
    SprayDraftIssue.Positive -> Res.string.spray_draft_positive
    SprayDraftIssue.AreaUnit -> Res.string.spray_draft_areaunit
    SprayDraftIssue.WaterUnit -> Res.string.spray_draft_waterunit
    SprayDraftIssue.Name -> Res.string.spray_draft_name
    SprayDraftIssue.QuantityUnit -> Res.string.spray_draft_quantityunit
    SprayDraftIssue.RateAmount -> Res.string.spray_draft_rateamount
    SprayDraftIssue.BasisAmount -> Res.string.spray_draft_basisamount
    SprayDraftIssue.RateUnit -> Res.string.spray_draft_rateunit
    SprayDraftIssue.RateBasis -> Res.string.spray_draft_ratebasis
})

@Composable
internal fun localizedSprayIssue(issue: SprayIssue): String = when (issue) {
    is SprayIssue.Failure -> localizedAppErrorMessage(issue.error)
    SprayIssue.ReviewFields -> stringResource(Res.string.spray_issue_review)
    SprayIssue.DraftConflict -> stringResource(Res.string.spray_issue_draft_conflict)
    SprayIssue.DocumentSaveFailed -> stringResource(Res.string.spray_issue_document_failed)
}
