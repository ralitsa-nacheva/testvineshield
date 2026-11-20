package com.rncoding.testvineshield.core.presentation.error

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.StorageError
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun localizedDatabaseError(error: DatabaseError): String = stringResource(when (error) {
    is DatabaseError.NotFound -> Res.string.local_error_missing
    is DatabaseError.ConstraintViolation -> Res.string.local_error_conflict
    is DatabaseError.TransactionFailed -> Res.string.local_error_transaction
    is DatabaseError.Unknown -> Res.string.local_error_database
})

@Composable
internal fun localizedStorageError(error: StorageError): String = stringResource(when (error) {
    is StorageError.ReadError -> Res.string.local_error_session_read
    is StorageError.WriteError -> Res.string.local_error_session_write
    is StorageError.DeleteError -> Res.string.local_error_session_delete
    is StorageError.CorruptedData -> Res.string.local_error_session_corrupted
})
