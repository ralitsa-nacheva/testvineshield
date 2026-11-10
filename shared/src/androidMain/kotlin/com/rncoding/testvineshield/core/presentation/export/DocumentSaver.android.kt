package com.rncoding.testvineshield.core.presentation.export

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.rncoding.testvineshield.core.domain.export.PreparedHarvestExport
import kotlinx.coroutines.*

@Composable
actual fun rememberDocumentSaver(): DocumentSaver {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<CompletableDeferred<Uri?>?>(null) }
    var saving by remember { mutableStateOf(false) }
    val selected: (Uri?) -> Unit = { uri ->
        val request = pending
        pending = null
        if (request?.complete(uri) != true && uri != null) scope.launch(Dispatchers.IO) {
            // A cancelled request must not feed a later request's writer.
            try { DocumentsContract.deleteDocument(resolver, uri) } catch (_: Exception) { /* Best effort. */ }
        }
    }
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json"), selected)
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv"), selected)
    return remember(resolver, scope, json, csv) {
        object : DocumentSaver {
            override suspend fun save(document: PreparedHarvestExport): DocumentSaveResult {
                val task = scope.async<DocumentSaveResult> {
                    if (saving || pending != null) return@async DocumentSaveResult.Failed("Another export is already open.")
                    saving = true
                    val selection = CompletableDeferred<Uri?>()
                    pending = selection
                    var destination: Uri? = null
                    var completed = false
                    var launched = false
                    try {
                        when (document.mimeType) {
                            "application/json" -> json.launch(document.fileName)
                            "text/csv" -> csv.launch(document.fileName)
                            else -> return@async DocumentSaveResult.Failed("Unsupported export format.")
                        }
                        launched = true
                        val selected = selection.await() ?: return@async DocumentSaveResult.Cancelled
                        destination = selected
                        withContext(Dispatchers.IO) {
                            currentCoroutineContext().ensureActive()
                            val bytes = document.content.encodeToByteArray()
                            val output = resolver.openOutputStream(selected, "w") ?: error("Document provider returned no stream.")
                            output.use {
                                var offset = 0
                                while (offset < bytes.size) {
                                    currentCoroutineContext().ensureActive()
                                    val count = minOf(16 * 1024, bytes.size - offset)
                                    it.write(bytes, offset, count)
                                    offset += count
                                }
                                it.flush()
                            }
                            currentCoroutineContext().ensureActive()
                        }
                        completed = true
                        DocumentSaveResult.Saved
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        DocumentSaveResult.Failed("The file could not be saved. Check the destination and try again.")
                    } finally {
                        saving = false
                        if (pending === selection) {
                            if (!launched) pending = null else selection.cancel()
                        }
                        // ACTION_CREATE_DOCUMENT creates a new document; clean a failed partial
                        // export where the provider supports deletion. Never delete on success.
                        val uri = destination
                        if (!completed && uri != null) withContext(NonCancellable + Dispatchers.IO) {
                            try { DocumentsContract.deleteDocument(resolver, uri) } catch (_: Exception) { /* Best effort; provider may refuse. */ }
                        }
                    }
                }
                return try { task.await() } finally { task.cancel() }
            }
        }
    }
}
