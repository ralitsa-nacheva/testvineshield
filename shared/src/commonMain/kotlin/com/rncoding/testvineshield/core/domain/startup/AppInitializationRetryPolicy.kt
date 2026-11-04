package com.rncoding.testvineshield.core.domain.startup

object AppInitializationRetryPolicy {
    // runAttemptCount starts at zero: one initial execution plus three retries.
    private const val MAX_RETRIES = 3

    fun shouldRetry(runAttemptCount: Int, failure: Exception): Boolean {
        // Catalogue invariants cannot be repaired by scheduling the same work again.
        if (failure is IllegalArgumentException || failure is IllegalStateException) {
            return false
        }
        return runAttemptCount in 0 until MAX_RETRIES
    }
}
