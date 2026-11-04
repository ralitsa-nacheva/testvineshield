package com.rncoding.testvineshield.core.domain.startup

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppInitializationRetryPolicyTest {
    @Test
    fun invariantFailuresAreNotRetried() {
        assertFalse(AppInitializationRetryPolicy.shouldRetry(0, IllegalStateException()))
        assertFalse(AppInitializationRetryPolicy.shouldRetry(0, IllegalArgumentException()))
    }

    @Test
    fun otherFailuresHaveBoundedRetries() {
        val failure = Exception("Temporary storage failure")
        for (attempt in 0..2) {
            assertTrue(AppInitializationRetryPolicy.shouldRetry(attempt, failure))
        }
        assertFalse(AppInitializationRetryPolicy.shouldRetry(3, failure))
        assertFalse(AppInitializationRetryPolicy.shouldRetry(100, failure))
        assertFalse(AppInitializationRetryPolicy.shouldRetry(-1, failure))
    }
}
