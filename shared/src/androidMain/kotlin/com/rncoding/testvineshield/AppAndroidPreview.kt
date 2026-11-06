package com.rncoding.testvineshield

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.rncoding.testvineshield.core.presentation.root.LoadingScreen

/** Preview only. The launcher belongs exclusively to androidApp.
 * Preview presentation without starting Koin, session restore or database work.
 */
@Preview(showBackground = true)
@Composable
fun AppAndroidPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            LoadingScreen("Preparing your local records…")
        }
    }
}
