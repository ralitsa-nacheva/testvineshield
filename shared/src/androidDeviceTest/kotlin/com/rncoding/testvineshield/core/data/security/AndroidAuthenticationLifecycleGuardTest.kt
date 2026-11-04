package com.rncoding.testvineshield.core.data.security

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AndroidAuthenticationLifecycleGuardTest {
    @Test
    fun stopEndsAttemptExactlyOnceEvenWhenDestroyFollows() = onMain {
        val owner = TestOwner().apply { resume() }
        var calls = 0
        AndroidAuthenticationLifecycleGuard(owner.lifecycle) { calls++ }
        owner.event(Lifecycle.Event.ON_PAUSE)
        assertEquals(0, calls)
        owner.event(Lifecycle.Event.ON_STOP)
        owner.event(Lifecycle.Event.ON_DESTROY)
        assertEquals(1, calls)
    }

    @Test
    fun disposingBeforeStopSuppressesCallback() = onMain {
        val owner = TestOwner().apply { resume() }
        var calls = 0
        val guard = AndroidAuthenticationLifecycleGuard(owner.lifecycle) { calls++ }
        guard.dispose()
        guard.dispose()
        owner.event(Lifecycle.Event.ON_PAUSE)
        owner.event(Lifecycle.Event.ON_STOP)
        owner.event(Lifecycle.Event.ON_DESTROY)
        assertEquals(0, calls)
    }

    @Test
    fun pauseAloneDoesNotCancelTheAttempt() = onMain {
        val owner = TestOwner().apply { resume() }
        var calls = 0
        val guard = AndroidAuthenticationLifecycleGuard(owner.lifecycle) { calls++ }
        owner.event(Lifecycle.Event.ON_PAUSE)
        owner.event(Lifecycle.Event.ON_RESUME)
        assertEquals(0, calls)
        guard.dispose()
    }

    @Test
    fun registeringGuardOnCreatedHostDoesNotImmediatelyCancel() = onMain {
        val owner = TestOwner()
        owner.event(Lifecycle.Event.ON_CREATE)
        var calls = 0
        val guard = AndroidAuthenticationLifecycleGuard(owner.lifecycle) { calls++ }
        assertEquals(0, calls)
        owner.event(Lifecycle.Event.ON_DESTROY)
        assertEquals(1, calls)
        guard.dispose()
    }

    private fun onMain(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { block() }
    }

    private class TestOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        fun event(event: Lifecycle.Event) = registry.handleLifecycleEvent(event)
        fun resume() {
            event(Lifecycle.Event.ON_CREATE)
            event(Lifecycle.Event.ON_START)
            event(Lifecycle.Event.ON_RESUME)
        }
    }
}
