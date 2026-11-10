package com.rncoding.testvineshield

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.runtime.CompositionLocalProvider
import com.rncoding.testvineshield.core.presentation.export.LocalDocumentExportController
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        CompositionLocalProvider(LocalDocumentExportController provides controller) { App() }
    }
    return controller
}
