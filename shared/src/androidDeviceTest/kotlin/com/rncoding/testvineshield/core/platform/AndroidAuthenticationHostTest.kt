package com.rncoding.testvineshield.core.platform

import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.security.AndroidLocalAuthenticator
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.SecurityError
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(AndroidJUnit4::class)
class AndroidAuthenticationHostTest {
    @Test
    fun providerReleasesTheMatchingActivity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val provider = AndroidActivityProvider()
            val activity = FragmentActivity()
            provider.setActivity(activity)
            assertSame(activity, provider.getActivity())
            provider.clearActivity(activity)
            assertNull(provider.getActivity())
        }
    }

    @Test
    fun oldActivityCannotClearItsReplacement() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val provider = AndroidActivityProvider()
            val old = FragmentActivity()
            val replacement = FragmentActivity()
            provider.setActivity(old)
            provider.setActivity(replacement)
            provider.clearActivity(old)
            assertSame(replacement, provider.getActivity())
        }
    }

    @Test
    fun missingActivityMeansUnavailable(): Unit = runBlocking {
        val authenticator = AndroidLocalAuthenticator(AndroidActivityProvider())
        val result = assertIs<Result.Success<Boolean>>(authenticator.isAvailable())
        assertEquals(false, result.data)
    }

    @Test
    fun missingActivityCannotAuthenticate(): Unit = runBlocking {
        val authenticator = AndroidLocalAuthenticator(AndroidActivityProvider())
        val result = assertIs<Result.Error<*>>(authenticator.authenticate("Test unlock"))
        assertEquals(SecurityError.BiometricNotAvailable, result.error)
    }

    @Test
    fun unresumedHostCannotAuthenticate(): Unit = runBlocking {
        val provider = AndroidActivityProvider()
        lateinit var host: FragmentActivity
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            host = FragmentActivity()
            provider.setActivity(host)
        }
        try {
            val result = assertIs<Result.Error<*>>(
                AndroidLocalAuthenticator(provider).authenticate("Test unlock")
            )
            assertEquals(SecurityError.BiometricNotAvailable, result.error)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                provider.clearActivity(host)
            }
        }
    }

}
