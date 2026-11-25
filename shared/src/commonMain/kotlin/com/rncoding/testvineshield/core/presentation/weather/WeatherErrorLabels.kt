package com.rncoding.testvineshield.core.presentation.weather

import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.core.presentation.error.localizedNetworkError
import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.disease_risk.MonitoringDraftSaveOutcome
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Map error identity, never English text or debug details. Other types use the shared localized fallback. */
@Composable
internal fun localizedWeatherError(error: AppError): String {
    if (error is NetworkError) return localizedNetworkError(error)
    val resource = when (error) {
        is WeatherHistoryError.InvalidInput -> Res.string.weather_error_location
        is WeatherHistoryError.TimeZoneMismatch -> Res.string.weather_error_zone
        is WeatherHistoryError.EvaluationFailed -> Res.string.weather_error_evaluation
        WeatherCacheSourceChanged -> Res.string.weather_error_source_changed
        is DatabaseError.NotFound -> Res.string.weather_error_missing_data
        is DatabaseError.ConstraintViolation -> Res.string.weather_error_conflict
        is DatabaseError.TransactionFailed -> Res.string.weather_error_transaction
        is DatabaseError.Unknown -> Res.string.weather_error_database
        AuthError.NoActiveSession -> Res.string.weather_error_login
        AuthError.Unauthorized, AuthError.SessionExpired -> Res.string.weather_error_session
        is UnexpectedError -> Res.string.weather_error_unexpected
        is BlockError.InvalidInput -> Res.string.weather_error_block_input
        BlockError.NotFound -> Res.string.weather_error_block_missing
        else -> null
    }
    return if (resource != null) stringResource(resource) else localizedAppErrorMessage(error)
}

@Composable
internal fun localizedWeatherDraftNotice(notice: WeatherDraftNotice): String = when (notice) {
    is WeatherDraftNotice.Failure -> localizedWeatherError(notice.error)
    is WeatherDraftNotice.Outcome -> stringResource(when (notice.value) {
        MonitoringDraftSaveOutcome.SAVED -> Res.string.weather_draft_saved
        MonitoringDraftSaveOutcome.BLOCK_UNAVAILABLE -> Res.string.weather_draft_block_missing
        MonitoringDraftSaveOutcome.DISEASE_UNAVAILABLE -> Res.string.weather_draft_disease_missing
        MonitoringDraftSaveOutcome.ALREADY_ENABLED -> Res.string.weather_draft_enabled
    })
}
