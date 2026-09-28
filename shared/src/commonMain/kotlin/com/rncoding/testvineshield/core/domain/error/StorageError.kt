package com.rncoding.testvineshield.core.domain.error

sealed interface StorageError : AppError {

    data class ReadError(
        override val cause: Throwable? = null
    ) : StorageError {

        override val userMessage: String =
            "Unable to read your saved session."

        override val debugMessage: String =
            "Secure storage read operation failed."
    }

    data class WriteError(
        override val cause: Throwable? = null
    ) : StorageError {

        override val userMessage: String =
            "Unable to save your session."

        override val debugMessage: String =
            "Secure storage write operation failed."
    }

    data class DeleteError(
        override val cause: Throwable? = null
    ) : StorageError {

        override val userMessage: String =
            "Unable to clear your saved session."

        override val debugMessage: String =
            "Secure storage delete operation failed."
    }

    data class CorruptedData(
        override val cause: Throwable? = null
    ) : StorageError {

        override val userMessage: String =
            "Your saved session could not be restored."

        override val debugMessage: String =
            "Stored session data is invalid or corrupted."
    }
}