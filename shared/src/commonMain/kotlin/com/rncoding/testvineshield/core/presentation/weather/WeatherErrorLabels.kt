package com.rncoding.testvineshield.core.presentation.weather

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.disease_risk.MonitoringDraftSaveOutcome
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Map error identity, never English text or debug details. Unknown types keep their safe message. */
@Composable
internal fun localizedWeatherError(error: AppError): String {
    val resource = when (error) {
        NetworkError.NoInternet -> Res.string.weather_error_network
        NetworkError.Timeout -> Res.string.weather_error_timeout
        is NetworkError.HttpError -> when (error.code) {
            400 -> Res.string.weather_error_request
            401 -> Res.string.weather_error_auth_required
            403 -> Res.string.weather_error_forbidden
            404 -> Res.string.weather_error_resource
            429 -> Res.string.weather_error_rate
            in 500..599 -> Res.string.weather_error_server
            else -> Res.string.weather_error_network_other
        }
        is NetworkError.SerializationError -> Res.string.weather_error_response
        is NetworkError.Unknown -> Res.string.weather_error_network_other
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
    return if (resource != null) stringResource(resource) else error.userMessage
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
