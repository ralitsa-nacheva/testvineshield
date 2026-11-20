package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.presentation.user_auth.localizedAuthErrorMessage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Localize known UI-safe errors by type; preserve other errors' existing safe message. */
@Composable
internal fun localizedAppErrorMessage(error: AppError): String = when (error) {
    AppInitializationError -> stringResource(Res.string.startup_data_unavailable)
    is AuthError, is SecurityError, is ValidationError -> localizedAuthErrorMessage(error)
    is UnexpectedError -> stringResource(Res.string.error_unexpected)
    else -> error.userMessage
}
