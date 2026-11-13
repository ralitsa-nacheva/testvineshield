package com.rncoding.testvineshield.core.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** One inbox per activity or scene. Access it on the UI thread. */
class VineyardLinkInbox {
    class Request internal constructor(val vineyardId: Long)

    var pending by mutableStateOf<Request?>(null)
        private set

    fun offer(vineyardId: Long) {
        if (vineyardId > 0) pending = Request(vineyardId)
    }

    fun consume(request: Request): Boolean {
        if (pending !== request) return false
        pending = null
        return true
    }
}
