package com.rncoding.testvineshield.core.presentation.disease_observations

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

sealed interface ObservationIssue {
    data class Failure(val error: AppError) : ObservationIssue
    data object LoadFailed : ObservationIssue
    data object SessionUnavailable : ObservationIssue
}

@Composable
internal fun localizedObservationIssue(issue: ObservationIssue): String = when (issue) {
    is ObservationIssue.Failure -> localizedAppErrorMessage(issue.error)
    ObservationIssue.LoadFailed -> stringResource(Res.string.observation_error_load)
    ObservationIssue.SessionUnavailable -> stringResource(Res.string.observation_error_session)
}
