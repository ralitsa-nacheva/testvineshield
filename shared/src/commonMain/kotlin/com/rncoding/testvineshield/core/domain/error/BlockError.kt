
package com.rncoding.testvineshield.core.domain.error

sealed interface BlockError : AppError {

    data class InvalidInput(
        override val userMessage: String
    ) : BlockError {
        override val debugMessage: String? = userMessage
        override val cause: Throwable? = null
    }

    data object NotFound : BlockError {
        override val userMessage =
            "The requested block could not be found."
        override val debugMessage =
            "Block not found in the selected vineyard."
        override val cause: Throwable? = null
    }
}
