package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AppInitializationError
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Localize known UI-safe errors by type; preserve other errors' existing safe message. */
@Composable
internal fun localizedAppErrorMessage(error: AppError): String = when (error) {
    AppInitializationError -> stringResource(Res.string.startup_data_unavailable)
    else -> error.userMessage
}
