package com.rncoding.testvineshield.core.presentation.security

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Preserve an issue's meaning until presentation resolves the active language. */
sealed interface SecuritySettingsIssue {
    data class Failure(val error: AppError) : SecuritySettingsIssue
    data object MethodUnavailable : SecuritySettingsIssue
}

@Composable
internal fun localizedSecuritySettingsIssue(issue: SecuritySettingsIssue): String = when (issue) {
    is SecuritySettingsIssue.Failure -> localizedAppErrorMessage(issue.error)
    SecuritySettingsIssue.MethodUnavailable -> stringResource(Res.string.security_method_unavailable)
}
