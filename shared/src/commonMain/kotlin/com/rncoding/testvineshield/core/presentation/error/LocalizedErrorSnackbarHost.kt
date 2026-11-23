package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.AppError

/** The queue retains error identity. Its safe fallback also supports ordinary hosts. */
internal data class ErrorSnackbarVisuals(val error: AppError) : SnackbarVisuals {
    override val message: String get() = error.userMessage
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/** Translate during composition without replacing the queued event or its callbacks. */
@Composable
internal fun LocalizedErrorSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        val error = (data.visuals as? ErrorSnackbarVisuals)?.error
        val translatedMessage = if (error != null) localizedAppErrorMessage(error) else data.visuals.message
        val translatedVisuals = object : SnackbarVisuals by data.visuals {
            override val message = translatedMessage
        }
        val translatedData = object : SnackbarData by data {
            override val visuals = translatedVisuals
        }
        Snackbar(snackbarData = translatedData)
    }
}
