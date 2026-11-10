package com.rncoding.testvineshield.core.data.repository

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRefreshCoordinatorTest {
    @Test fun sameVineyardRequestsDoNotOverlapAndReleaseAfterFailure() = runTest {
        val coordinator=WeatherRefreshCoordinator(); val entered=CompletableDeferred<Unit>(); val release=CompletableDeferred<Unit>()
        val events=mutableListOf<String>()
        val first=launch { coordinator.withRefresh(10) { events.add("first"); entered.complete(Unit); release.await(); events.add("first end") } }
        entered.await()
        val second=launch { coordinator.withRefresh(10) { events.add("second") } }
        runCurrent(); assertEquals(listOf("first"),events)
        release.complete(Unit); first.join(); second.join(); assertEquals(listOf("first","first end","second"),events)
        assertFailsWith<IllegalStateException> { coordinator.withRefresh(10) { error("failure") } }
        coordinator.withRefresh(10) { events.add("after failure") }
        assertEquals("after failure",events.last())
    }
    @Test fun cancellingWaiterNeverRunsItAndCancellingHolderReleasesStripe() = runTest {
        val coordinator=WeatherRefreshCoordinator(); val entered=CompletableDeferred<Unit>(); var waited=false
        val holder=launch { coordinator.withRefresh(10) { entered.complete(Unit); awaitCancellation() } }; entered.await()
        val waiter=launch { coordinator.withRefresh(10) { waited=true } }; runCurrent(); waiter.cancelAndJoin()
        assertFalse(waited); holder.cancelAndJoin()
        coordinator.withRefresh(10) { waited=true }; assertTrue(waited)
    }
    @Test fun separateStripesCanProgressAndInvalidIdsFail() = runTest {
        val coordinator=WeatherRefreshCoordinator(); val entered=CompletableDeferred<Unit>()
        val holder=launch { coordinator.withRefresh(10) { entered.complete(Unit); awaitCancellation() } }; entered.await()
        var other=false; coordinator.withRefresh(11) { other=true }; assertTrue(other)
        holder.cancelAndJoin(); assertFailsWith<IllegalArgumentException> { coordinator.withRefresh(0) { } }
    }
}
