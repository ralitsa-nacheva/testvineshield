
package com.rncoding.testvineshield.core.data.remote

import com.rncoding.testvineshield.core.domain.error.NetworkError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException

object WeatherNetworkErrorMapper {

    fun map(exception: Throwable): NetworkError {
        if (exception is CancellationException) {
            throw exception
        }

        return when (exception) {
            is HttpRequestTimeoutException ->
                NetworkError.Timeout

            is ResponseException ->
                NetworkError.HttpError(
                    code = exception.response.status.value,
                    debugMessage = exception.message,
                    cause = exception
                )

            is IOException ->
                NetworkError.NoInternet

            else ->
                NetworkError.Unknown(
                    cause = exception
                )
        }
    }
}
