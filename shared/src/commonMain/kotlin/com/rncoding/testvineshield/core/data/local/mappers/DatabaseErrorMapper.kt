package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.domain.error.DatabaseError

object DatabaseErrorMapper {

    fun map(
        exception: Throwable,
        operation: DatabaseError.Operation
    ): DatabaseError {

        return when {

            isConstraintViolation(exception) ->
                DatabaseError.ConstraintViolation(
                    operation = operation,
                    debugMessage =
                        exception.message
                            ?: "Database constraint violation",
                    cause = exception
                )

            isTransactionFailure(exception) ->
                DatabaseError.TransactionFailed(
                    debugMessage =
                        exception.message
                            ?: "Database transaction failed",
                    cause = exception
                )

            else ->
                DatabaseError.Unknown(
                    operation = operation,
                    cause = exception
                )
        }
    }

    private fun isConstraintViolation(
        exception: Throwable
    ): Boolean {

        val message =
            exception.message
                ?.lowercase()
                ?: return false

        return message.contains("constraint") ||
                message.contains("foreign key") ||
                message.contains("unique") ||
                message.contains("not null")
    }

    private fun isTransactionFailure(
        exception: Throwable
    ): Boolean {

        val message =
            exception.message
                ?.lowercase()
                ?: return false

        return message.contains("transaction")
    }
}