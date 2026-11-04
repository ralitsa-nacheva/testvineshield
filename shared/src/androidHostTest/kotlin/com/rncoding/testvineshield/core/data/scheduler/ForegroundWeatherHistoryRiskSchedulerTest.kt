package com.rncoding.testvineshield.core.data.scheduler

import com.rncoding.testvineshield.core.domain.scheduler.ForegroundWeatherOutcome
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ForegroundWeatherHistoryRiskSchedulerTest {
    private fun TestScope.scheduler(execute: suspend (Long, Long) -> ForegroundWeatherOutcome) =
        ForegroundWeatherHistoryRiskScheduler(backgroundScope, execute, 1_000, 100) { testScheduler.currentTime }

    @Test fun waitsUntilForegroundAndDeduplicates() = runTest {
        var calls = 0
        val scheduler = scheduler { _, _ -> calls++; ForegroundWeatherOutcome.SUCCESS }
        scheduler.schedule(1, 10); scheduler.schedule(1, 10); runCurrent()
        assertEquals(0, calls)
        scheduler.setForeground(true); runCurrent()
        assertEquals(1, calls)
        scheduler.schedule(1, 10); scheduler.setForeground(true); runCurrent()
        assertEquals(1, calls)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(2, calls)
    }

    @Test fun resumingDoesNotBypassInterval() = runTest {
        var calls = 0
        val scheduler = scheduler { _, _ -> calls++; ForegroundWeatherOutcome.SUCCESS }
        scheduler.schedule(1, 10); scheduler.setForeground(true); runCurrent()
        scheduler.setForeground(false); runCurrent()
        advanceTimeBy(500)
        scheduler.setForeground(true); runCurrent()
        assertEquals(1, calls)
        advanceTimeBy(500); runCurrent()
        assertEquals(2, calls)
    }

    @Test fun inactiveCancelsInFlightAndResumeRestartsIt() = runTest {
        var entered = 0; var cancelled = 0
        val scheduler = scheduler { _, _ ->
            entered++
            try { awaitCancellation() } finally { cancelled++ }
        }
        scheduler.schedule(1, 10); scheduler.setForeground(true); runCurrent()
        scheduler.setForeground(false); runCurrent()
        assertEquals(1, cancelled)
        scheduler.setForeground(true); runCurrent()
        assertEquals(2, entered)
    }

    @Test fun logoutCancelsOnlyThatOwnersWork() = runTest {
        val calls = mutableListOf<Long>()
        val scheduler = scheduler { owner, _ -> calls += owner; ForegroundWeatherOutcome.SUCCESS }
        scheduler.schedule(1, 10); scheduler.schedule(2, 20)
        scheduler.cancelAllForUser(1); scheduler.setForeground(true); runCurrent()
        assertEquals(listOf(2L), calls)
        advanceTimeBy(1_000); runCurrent()
        assertEquals(listOf(2L, 2L), calls)
    }

    @Test fun vineyardDeletionRemovesOnlyThatRegistration() = runTest {
        val calls = mutableListOf<Long>()
        val scheduler = scheduler { _, vineyard -> calls += vineyard; ForegroundWeatherOutcome.SUCCESS }
        scheduler.schedule(1, 10); scheduler.schedule(1, 20); scheduler.cancel(1, 10)
        scheduler.setForeground(true); runCurrent()
        assertEquals(listOf(20L), calls)
    }

    @Test fun transientRetriesAreBoundedThenUseNormalInterval() = runTest {
        var calls = 0
        val scheduler = scheduler { _, _ -> calls++; ForegroundWeatherOutcome.RETRY }
        scheduler.schedule(1, 10); scheduler.setForeground(true); runCurrent()
        repeat(3) { advanceTimeBy(100); runCurrent() }
        assertEquals(4, calls)
        advanceTimeBy(999); runCurrent(); assertEquals(4, calls)
        advanceTimeBy(1); runCurrent(); assertEquals(5, calls)
    }

    @Test fun unauthorizedWorkIsRemovedWithoutARefreshLoop() = runTest {
        var calls = 0
        val scheduler = scheduler { _, _ -> calls++; ForegroundWeatherOutcome.REMOVE }
        scheduler.schedule(1, 10); scheduler.schedule(1, 20); runCurrent()
        scheduler.setForeground(true); runCurrent()
        advanceTimeBy(10_000); runCurrent()
        assertEquals(2, calls) // Also guards removal during foreground iteration.
    }

    @Test fun unexpectedFailureDoesNotSpinOrStopOtherVineyards() = runTest {
        val calls = mutableListOf<Long>()
        val scheduler = scheduler { _, vineyard ->
            calls += vineyard
            if (vineyard == 10L) error("failure")
            ForegroundWeatherOutcome.SUCCESS
        }
        scheduler.schedule(1, 10); scheduler.schedule(1, 20); scheduler.setForeground(true); runCurrent()
        assertEquals(listOf(10L, 20L), calls)
        advanceTimeBy(999); runCurrent(); assertEquals(2, calls.size)
    }

    @Test fun executionsAreSerializedAcrossVineyards() = runTest {
        var running = 0; var maximum = 0; var entered = 0
        val scheduler = scheduler { _, _ ->
            running++; entered++; maximum = maxOf(maximum, running)
            delay(100)
            running--
            ForegroundWeatherOutcome.SUCCESS
        }
        scheduler.schedule(1, 10); scheduler.schedule(1, 20); scheduler.setForeground(true); runCurrent()
        assertEquals(1, entered)
        advanceTimeBy(100); runCurrent()
        assertEquals(2, entered); assertEquals(1, maximum)
    }

    @Test fun cancelAndRecreateCannotReviveOldRegistration() = runTest {
        var calls = 0
        val scheduler = scheduler { _, _ -> calls++; ForegroundWeatherOutcome.SUCCESS }
        scheduler.schedule(1, 10); scheduler.setForeground(true); runCurrent()
        scheduler.cancel(1, 10); scheduler.schedule(1, 10); runCurrent()
        advanceTimeBy(1_000); runCurrent()
        assertEquals(3, calls)
    }

    @Test fun invalidIdentifiersAreRejectedBeforeLaunching() = runTest {
        val scheduler = scheduler { _, _ -> error("must not run") }
        assertFailsWith<IllegalArgumentException> { scheduler.schedule(0, 10) }
        assertFailsWith<IllegalArgumentException> { scheduler.schedule(1, -1) }
    }
}
