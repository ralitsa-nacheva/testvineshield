package com.rncoding.testvineshield.core.presentation.account

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Preserve an issue's meaning until presentation resolves the active language. */
sealed interface AccountIssue {
    data class Failure(val error: AppError) : AccountIssue
    data object PasswordMismatch : AccountIssue
}

@Composable
internal fun localizedAccountIssue(issue: AccountIssue): String = when (issue) {
    is AccountIssue.Failure -> localizedAppErrorMessage(issue.error)
    AccountIssue.PasswordMismatch -> stringResource(Res.string.account_password_mismatch)
}
