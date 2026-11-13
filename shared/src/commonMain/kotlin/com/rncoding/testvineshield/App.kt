package com.rncoding.testvineshield

import com.rncoding.testvineshield.core.presentation.navigation.VineyardLinkInbox
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rncoding.testvineshield.core.presentation.root.AppRoot
import com.rncoding.testvineshield.core.presentation.theme.VineShieldTheme

@Composable
fun App(
    modifier: Modifier = Modifier,
    vineyardLinks: VineyardLinkInbox? = null
) {
    VineShieldTheme {
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppRoot(
                modifier = Modifier.fillMaxSize(),
                vineyardLinks = vineyardLinks
            )
        }
    }
}
