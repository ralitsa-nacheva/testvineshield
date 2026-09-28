package com.rncoding.testvineshield.core.domain.error

//import com.rncoding.testvineshield.core.domain.error.Error

sealed interface Result<out D, out E : AppError> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : AppError>(val error: E) : Result<Nothing, E>
}