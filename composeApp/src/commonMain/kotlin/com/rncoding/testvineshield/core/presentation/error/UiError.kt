package com.rncoding.testvineshield.core.presentation.error

sealed interface UiError {
    data class Snackbar(
        val message: String,
        val action: String? = null
    ) : UiError
    data class Dialog(
        val title: String,
        val message: String,
        val confirmText: String = "OK"
    ) : UiError
    data class Inline(
        val field: String,
        val message: String
    ) : UiError
}
