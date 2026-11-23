package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.NetworkError
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

/** Shared service-neutral text; never render HTTP diagnostics or exception causes. */
@Composable
internal fun localizedNetworkError(error: NetworkError): String = stringResource(when (error) {
    NetworkError.NoInternet -> Res.string.network_error_unreachable
    NetworkError.Timeout -> Res.string.network_error_timeout
    is NetworkError.HttpError -> when (error.code) {
        400 -> Res.string.network_error_request
        401 -> Res.string.network_error_auth
        403 -> Res.string.network_error_forbidden
        404 -> Res.string.network_error_missing
        429 -> Res.string.network_error_rate
        in 500..599 -> Res.string.network_error_server
        else -> Res.string.network_error_unknown
    }
    is NetworkError.SerializationError -> Res.string.network_error_invalid_response
    is NetworkError.Unknown -> Res.string.network_error_unknown
})
