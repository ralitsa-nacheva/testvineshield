package com.rncoding.testvineshield.core.presentation.error

sealed interface UiError {

    data class Snackbar(
        val message: String,
        val action: Action? = null
    ) : UiError

    data class Dialog(
        val title: String,
        val message: String,
        val confirmText: String = "OK"
    ) : UiError

    data class Inline(
        val field: Field,
        val message: String
    ) : UiError

    enum class Action {
        RETRY,
        LOGIN,
        UNLOCK
    }

    enum class Field {
        EMAIL,
        PASSWORD,
        NAME,
        VINEYARD_NAME
    }
}