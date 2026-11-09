package com.rncoding.testvineshield.core.data.security

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/** Ends one authentication attempt when its host stops. Access only on Main. */
internal class AndroidAuthenticationLifecycleGuard(
    private val lifecycle: Lifecycle,
    private val cancelOnStop: Boolean = true,
    private val onHostStopped: () -> Unit
) : LifecycleEventObserver {
    private var disposed = false

    init {
        lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (!disposed && ((cancelOnStop && event == Lifecycle.Event.ON_STOP) || event == Lifecycle.Event.ON_DESTROY)) {
            dispose()
            onHostStopped()
        }
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        lifecycle.removeObserver(this)
    }
}
