package com.rncoding.testvineshield

import android.content.Intent
import android.os.Bundle
import com.rncoding.testvineshield.core.presentation.navigation.VineyardLinkInbox
import com.rncoding.testvineshield.core.presentation.navigation.parseVineyardDeepLink
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {
    private val activityProvider: AndroidActivityProvider by inject()

    private val vineyardLinks = VineyardLinkInbox()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityProvider.setActivity(this)
        val pendingId = if (savedInstanceState == null) {
            vineyardIdFrom(intent)
        } else {
            savedInstanceState.getLong(PENDING_VINEYARD_ID).takeIf { it > 0 }
        }
        pendingId?.let(vineyardLinks::offer)
        setContent { App(vineyardLinks = vineyardLinks) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vineyardIdFrom(intent)?.let(vineyardLinks::offer)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        vineyardLinks.pending?.vineyardId?.let { outState.putLong(PENDING_VINEYARD_ID, it) }
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
