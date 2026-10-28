
package com.rncoding.testvineshield.core.domain.error

sealed interface HarvestError : AppError {

    data class InvalidInput(
        override val userMessage: String
    ) : HarvestError {
        override val debugMessage: String? = userMessage
        override val cause: Throwable? = null
    }

    data object NotFound : HarvestError {
        override val userMessage =
            "The requested harvest could not be found."

        override val debugMessage =
            "Harvest not found in the selected block."

        override val cause: Throwable? = null
    }
}
