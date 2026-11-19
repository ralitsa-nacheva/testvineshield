package com.rncoding.testvineshield

import android.content.Intent
import android.os.Bundle
import com.rncoding.testvineshield.core.presentation.navigation.VineyardLinkInbox
import com.rncoding.testvineshield.core.presentation.navigation.parseVineyardLinkTarget
import com.rncoding.testvineshield.core.presentation.navigation.VineyardLinkTarget
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.rncoding.testvineshield.core.platform.AndroidActivityProvider
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {
    private val activityProvider: AndroidActivityProvider by inject()

    private val vineyardLinks = VineyardLinkInbox()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityProvider.setActivity(this)
        val target = if (savedInstanceState == null) {
            targetFrom(intent)
        } else {
            savedInstanceState.getLong(PENDING_VINEYARD_ID).takeIf { it > 0 }?.let { vineyardId ->
                VineyardLinkTarget(
                    vineyardId,
                    savedInstanceState.getLong(PENDING_BLOCK_ID).takeIf { it > 0 }
                )
            }
        }
        target?.let { vineyardLinks.offer(it.vineyardId, it.blockId) }
        setContent { App(vineyardLinks = vineyardLinks) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        targetFrom(intent)?.let { vineyardLinks.offer(it.vineyardId, it.blockId) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        vineyardLinks.pending?.let { request ->
            outState.putLong(PENDING_VINEYARD_ID, request.vineyardId)
            request.blockId?.let { outState.putLong(PENDING_BLOCK_ID, it) }
        }
    }

    private fun targetFrom(intent: Intent): VineyardLinkTarget? =
        if (intent.action == Intent.ACTION_VIEW) parseVineyardLinkTarget(intent.dataString) else null

    private companion object {
        const val PENDING_BLOCK_ID = "pendingBlockId"
        const val PENDING_VINEYARD_ID = "pendingVineyardId"
    }

    override fun onDestroy() {
        // The provider clears only this instance, never a newer replacement.
        activityProvider.clearActivity(this)
        super.onDestroy()
    }
}
