package com.rncoding.testvineshield.core.presentation.export

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.export.PreparedDocument

sealed interface DocumentSaveResult {
    data object Saved : DocumentSaveResult
    data object Cancelled : DocumentSaveResult
    data class Failed(val message: String) : DocumentSaveResult
}

interface DocumentSaver {
    /** The implementation belongs to the current screen/window; disposal cancels it. */
    suspend fun save(document: PreparedDocument): DocumentSaveResult
}

@Composable
expect fun rememberDocumentSaver(): DocumentSaver
