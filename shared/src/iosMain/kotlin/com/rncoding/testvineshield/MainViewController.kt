package com.rncoding.testvineshield

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import com.rncoding.testvineshield.core.presentation.export.LocalDocumentExportController
import com.rncoding.testvineshield.core.presentation.navigation.parseVineyardDeepLink
import platform.UIKit.UIViewController

/** Owned by one SwiftUI scene and called on the main thread. */
class IosVineyardLinkReceiver {
    internal var pendingVineyardId by mutableStateOf<Long?>(null)
        private set

    fun receive(url: String) {
        parseVineyardDeepLink(url)?.let { pendingVineyardId = it }
    }

    internal fun handled(id: Long) {
        if (pendingVineyardId == id) pendingVineyardId = null
    }
}

fun MainViewController(linkReceiver: IosVineyardLinkReceiver): UIViewController {
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        CompositionLocalProvider(LocalDocumentExportController provides controller) {
            App(
                pendingVineyardId = linkReceiver.pendingVineyardId,
                onVineyardLinkHandled = linkReceiver::handled
            )
        }
    }
    return controller
}
