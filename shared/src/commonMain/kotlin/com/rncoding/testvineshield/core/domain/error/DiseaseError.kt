
package com.rncoding.testvineshield.core.domain.error

sealed interface DiseaseError : AppError {

    data class InvalidInput(
        override val userMessage: String
    ) : DiseaseError {
        override val debugMessage: String? = userMessage
        override val cause: Throwable? = null
    }

    data object NotFound : DiseaseError {
        override val userMessage =
            "The requested disease could not be found."

        override val debugMessage =
            "Disease record does not exist."

        override val cause: Throwable? = null
    }

    data object PredefinedDisease : DiseaseError {
        override val userMessage =
            "Predefined diseases cannot be modified or deleted."

        override val debugMessage =
            "Attempted to modify a protected predefined disease."

        override val cause: Throwable? = null
    }

    data object DiseaseInUse : DiseaseError {
        override val userMessage =
            "This disease cannot be deleted because it is used by existing records."

        override val debugMessage =
            "Disease is referenced by an occurrence or alert."

        override val cause: Throwable? = null
    }
}
