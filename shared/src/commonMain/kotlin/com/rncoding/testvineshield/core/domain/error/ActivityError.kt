
package com.rncoding.testvineshield.core.domain.error

sealed interface ActivityError : AppError {

    data class InvalidInput(
        override val userMessage: String
    ) : ActivityError {
        override val debugMessage: String? = userMessage
        override val cause: Throwable? = null
    }

    data object InvalidBlock : ActivityError {
        override val userMessage =
            "The selected block does not belong to this vineyard."
        override val debugMessage =
            "Activity block/vineyard relationship is invalid."
        override val cause: Throwable? = null
    }

    data object NotFound : ActivityError {
        override val userMessage =
            "The requested activity could not be found."

        override val debugMessage =
            "Activity not found or inaccessible in the requested vineyard."

        override val cause: Throwable? = null
    }
}
