package com.rncoding.testvineshield

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
    pendingVineyardId: Long? = null,
    onVineyardLinkHandled: (Long) -> Unit = {}
) {
    VineShieldTheme {
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AppRoot(
                modifier = Modifier.fillMaxSize(),
                pendingVineyardId = pendingVineyardId,
                onVineyardLinkHandled = onVineyardLinkHandled
            )
        }
    }
}
