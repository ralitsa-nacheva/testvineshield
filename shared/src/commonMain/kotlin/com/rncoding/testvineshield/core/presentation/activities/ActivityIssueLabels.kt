package com.rncoding.testvineshield.core.presentation.activities

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

enum class ActivityDraftIssue { TypeRequired, SeasonRequired, DateRequired, DateFormat, ClearCompletion }
sealed interface ActivityEditorIssue {
    data class Failure(val error: AppError) : ActivityEditorIssue
    data object InvalidScope : ActivityEditorIssue
    data object ReviewFields : ActivityEditorIssue
}

@Composable
internal fun localizedActivityDraftIssue(issue: ActivityDraftIssue): String = stringResource(when (issue) {
    ActivityDraftIssue.TypeRequired -> Res.string.activity_draft_typerequired
    ActivityDraftIssue.SeasonRequired -> Res.string.activity_draft_seasonrequired
    ActivityDraftIssue.DateRequired -> Res.string.activity_draft_daterequired
    ActivityDraftIssue.DateFormat -> Res.string.activity_draft_dateformat
    ActivityDraftIssue.ClearCompletion -> Res.string.activity_draft_clearcompletion
})

@Composable
internal fun localizedActivityEditorIssue(issue: ActivityEditorIssue): String = when (issue) {
    is ActivityEditorIssue.Failure -> localizedAppErrorMessage(issue.error)
    ActivityEditorIssue.InvalidScope -> stringResource(Res.string.activity_validation_invalidscope)
    ActivityEditorIssue.ReviewFields -> stringResource(Res.string.activity_review_fields)
}
