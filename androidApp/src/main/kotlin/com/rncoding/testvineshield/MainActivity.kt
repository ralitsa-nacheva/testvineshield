package com.rncoding.testvineshield

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rncoding.testvineshield.core.presentation.navigation.parseVineyardDeepLink
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {
    private val activityProvider: AndroidActivityProvider by inject()

    private var pendingVineyardId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityProvider.setActivity(this)
        pendingVineyardId = if (savedInstanceState == null) {
            vineyardIdFrom(intent)
        } else {
            savedInstanceState.getLong(PENDING_VINEYARD_ID).takeIf { it > 0 }
        }
        setContent {
            App(
                pendingVineyardId = pendingVineyardId,
                onVineyardLinkHandled = { id ->
                    if (pendingVineyardId == id) pendingVineyardId = null
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vineyardIdFrom(intent)?.let { pendingVineyardId = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingVineyardId?.let { outState.putLong(PENDING_VINEYARD_ID, it) }
    }

    private fun vineyardIdFrom(intent: Intent): Long? =
        if (intent.action == Intent.ACTION_VIEW) parseVineyardDeepLink(intent.dataString) else null

    private companion object {
        const val PENDING_VINEYARD_ID = "pendingVineyardId"
    }

    override fun onDestroy() {
        // The provider clears only this instance, never a newer replacement.
        activityProvider.clearActivity(this)
        super.onDestroy()
    }
}
