package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.presentation.user_auth.localizedAuthErrorMessage
import com.rncoding.testvineshield.core.presentation.vineyard_editor.localizedVineyardValidationError
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Resolve error identity at rendering time; unknown types receive a localized safe fallback. */
@Composable
internal fun localizedAppErrorMessage(error: AppError): String = when (error) {
    AppInitializationError -> stringResource(Res.string.startup_data_unavailable)
    is AuthError, is SecurityError, is ValidationError -> localizedAuthErrorMessage(error)
    is UnexpectedError -> stringResource(Res.string.error_unexpected)
    is DatabaseError -> localizedDatabaseError(error)
    is StorageError -> localizedStorageError(error)
    is NetworkError -> localizedNetworkError(error)
    is BlockError -> localizedBlockError(error)
    is VineyardValidationError -> localizedVineyardValidationError(error)
    is DiseaseError -> localizedDiseaseError(error)
    is HarvestError -> localizedHarvestError(error)
    is ActivityError -> if (error is ActivityError.InvalidInput && error.sprayReason != null) localizedSprayValidationError(error) else localizedActivityError(error)
    else -> stringResource(Res.string.error_action_failed)
}
