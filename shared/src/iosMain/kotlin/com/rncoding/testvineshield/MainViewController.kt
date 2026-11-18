package com.rncoding.testvineshield

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import com.rncoding.testvineshield.core.presentation.export.LocalDocumentExportController
import com.rncoding.testvineshield.core.presentation.navigation.parseVineyardLinkTarget
import com.rncoding.testvineshield.core.presentation.navigation.VineyardLinkInbox
import platform.UIKit.UIViewController

/** Owned by one SwiftUI scene and called on the main thread. */
class IosVineyardLinkReceiver {
    internal val inbox = VineyardLinkInbox()

    fun receive(url: String) {
        parseVineyardLinkTarget(url)?.let { inbox.offer(it.vineyardId, it.blockId) }
    }
}

fun MainViewController(linkReceiver: IosVineyardLinkReceiver): UIViewController {
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        CompositionLocalProvider(LocalDocumentExportController provides controller) {
            App(vineyardLinks = linkReceiver.inbox)
        }
    }
    return controller
}
