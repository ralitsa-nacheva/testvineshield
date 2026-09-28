package com.rncoding.testvineshield.core.domain.error

/**
 * Describes failures that originate from the local database.
 *
 * DatabaseError belongs to the domain error model.
 * It must not expose Room, SQLite, SQLCipher, or platform-specific
 * implementation types.
 */
sealed interface DatabaseError : AppError {

    /**
     * The database operation that was being performed when
     * the error occurred.
     */
    enum class Operation {
        READ,
        INSERT,
        UPDATE,
        DELETE,
        TRANSACTION
    }

    /**
     * The requested entity does not exist.
     */
    data class NotFound(
        val entity: String,
        val operation: Operation = Operation.READ
    ) : DatabaseError {

        override val userMessage: String =
            "The requested data could not be found."

        override val debugMessage: String =
            "Database entity not found: $entity " +
                    "(operation=$operation)"

        override val cause: Throwable? =
            null
    }

    /**
     * A database constraint prevented the operation.
     *
     * Examples:
     * - UNIQUE constraint
     * - FOREIGN KEY constraint
     * - NOT NULL constraint
     * - CHECK constraint
     */
    data class ConstraintViolation(
        val operation: Operation,
        override val debugMessage: String,
        override val cause: Throwable? = null
    ) : DatabaseError {

        override val userMessage: String =
            "The operation could not be completed because " +
                    "the data conflicts with existing information."
    }

    /**
     * A database transaction failed.
     */
    data class TransactionFailed(
        override val debugMessage: String,
        override val cause: Throwable? = null
    ) : DatabaseError {

        override val userMessage: String =
            "The operation could not be completed."
    }

    /**
     * An unexpected database failure.
     *
     * This should be the final fallback after known database
     * exceptions have been classified.
     */
    data class Unknown(
        val operation: Operation,
        override val cause: Throwable? = null
    ) : DatabaseError {

        override val userMessage: String =
            "Something went wrong while accessing your data."

        override val debugMessage: String =
            cause?.message
                ?: ("Unknown database error " +
                        "(operation=$operation)")
    }
}