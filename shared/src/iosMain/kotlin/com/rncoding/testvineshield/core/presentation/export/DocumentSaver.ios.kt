package com.rncoding.testvineshield.core.presentation.export

import androidx.compose.runtime.*
import com.rncoding.testvineshield.core.domain.export.PreparedHarvestExport
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.coroutines.resume

/** Explicit app-owned host: no global keyWindow selection or unrelated controller dismissal. */
internal val LocalDocumentExportController = staticCompositionLocalOf<UIViewController?> { null }

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberDocumentSaver(): DocumentSaver {
    val host = LocalDocumentExportController.current
    val scope = rememberCoroutineScope()
    return remember(host, scope) {
        object : DocumentSaver {
            private var busy = false
            // UIKit's delegate is weak; retain it for the entire picker operation.
            private var retainedDelegate: UIDocumentPickerDelegateProtocol? = null

            override suspend fun save(document: PreparedHarvestExport): DocumentSaveResult {
                val task = scope.async<DocumentSaveResult> {
                    if (busy) return@async DocumentSaveResult.Failed("Another export is already open.")
                    val controller = host ?: return@async DocumentSaveResult.Failed("The export window is unavailable. Reopen this screen.")
                    if (controller.view.window == null || controller.presentedViewController != null)
                        return@async DocumentSaveResult.Failed("Close the current dialog or window and try exporting again.")
                    busy = true
                    val directory = NSTemporaryDirectory() + "vineshield-export-" + NSUUID().UUIDString
                    var picker: UIDocumentPickerViewController? = null
                    try {
                        val url = withContext(Dispatchers.Default) {
                            val files = NSFileManager.defaultManager
                            check(files.createDirectoryAtPath(directory, true, null, null))
                            val bytes = document.content.encodeToByteArray()
                            val data = bytes.usePinned { pinned ->
                                NSData.create(bytes = pinned.addressOf(0), length = bytes.size.convert())
                            }
                            val path = directory + "/" + document.fileName
                            check(data.writeToFile(path, atomically = true))
                            NSURL.fileURLWithPath(path)
                        }
                        currentCoroutineContext().ensureActive()
                        val activePicker = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = true)
                        activePicker.modalPresentationStyle = UIModalPresentationFullScreen
                        picker = activePicker
                        suspendCancellableCoroutine<DocumentSaveResult> { continuation ->
                            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                                override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                                    if (continuation.isActive) continuation.resume(if (didPickDocumentsAtURLs.isNotEmpty()) DocumentSaveResult.Saved else DocumentSaveResult.Failed("No exported file was returned."))
                                }
                                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                                    if (continuation.isActive) continuation.resume(DocumentSaveResult.Cancelled)
                                }
                            }
                            retainedDelegate = delegate
                            activePicker.delegate = delegate
                            controller.presentViewController(activePicker, animated = true, completion = null)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        DocumentSaveResult.Failed("The file could not be exported. Check the destination and try again.")
                    } finally {
                        withContext(NonCancellable + Dispatchers.Main.immediate) {
                            val ownPicker = picker
                            if (ownPicker?.presentingViewController != null) ownPicker.dismissViewControllerAnimated(false, completion = null)
                            retainedDelegate = null
                            busy = false
                        }
                        withContext(NonCancellable + Dispatchers.Default) {
                            NSFileManager.defaultManager.removeItemAtPath(directory, null)
                        }
                    }
                }
                return try { task.await() } finally { task.cancel() }
            }
        }
    }
}
