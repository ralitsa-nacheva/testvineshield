
package com.rncoding.testvineshield.core.domain.scheduler

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WeatherHistoryBackgroundWorkCoordinatorTest {

    @Test
    fun schedulesAllVineyardsForUser() = runBlocking {
        val source = FakeVineyardSource(
            vineyardIds = listOf(10L, 20L, 30L)
        )
        val scheduler = FakeScheduler()

        val coordinator = WeatherHistoryBackgroundWorkCoordinator(
            vineyardSource = source,
            scheduler = scheduler
        )

        coordinator.scheduleForUser(userId = 1L)

        assertEquals(
            listOf(
                1L to 10L,
                1L to 20L,
                1L to 30L
            ),
            scheduler.scheduled
        )
    }

    @Test
    fun emptyVineyardListSchedulesNothing() = runBlocking {
        val source = FakeVineyardSource(emptyList())
        val scheduler = FakeScheduler()

        val coordinator = WeatherHistoryBackgroundWorkCoordinator(
            vineyardSource = source,
            scheduler = scheduler
        )

        coordinator.scheduleForUser(userId = 1L)

        assertEquals(emptyList(), scheduler.scheduled)
    }

    @Test
    fun repeatedSchedulingPassesSameVineyards() = runBlocking {
        val source = FakeVineyardSource(
            listOf(10L, 20L)
        )
        val scheduler = FakeScheduler()

        val coordinator = WeatherHistoryBackgroundWorkCoordinator(
            vineyardSource = source,
            scheduler = scheduler
        )

        coordinator.scheduleForUser(1L)
        coordinator.scheduleForUser(1L)

        assertEquals(
            listOf(
                1L to 10L,
                1L to 20L,
                1L to 10L,
                1L to 20L
            ),
            scheduler.scheduled
        )
    }

    @Test
    fun invalidUserIdIsRejected(): Unit = runBlocking {
        val coordinator = WeatherHistoryBackgroundWorkCoordinator(
            vineyardSource = FakeVineyardSource(emptyList()),
            scheduler = FakeScheduler()
        )

        assertFailsWith<IllegalArgumentException> {
            coordinator.scheduleForUser(0L)
        }
    }

    private class FakeVineyardSource(
        private val vineyardIds: List<Long>
    ) : BackgroundWorkVineyardSource {

        override suspend fun getVineyardIds(
            userId: Long
        ): List<Long> = vineyardIds
    }

    private class FakeScheduler : WeatherHistoryRiskScheduler {

        val scheduled = mutableListOf<Pair<Long, Long>>()

        override fun schedule(
            userId: Long,
            vineyardId: Long
        ) {
            scheduled.add(userId to vineyardId)
        }

        override fun cancel(
            userId: Long,
            vineyardId: Long
        ) = Unit

        override fun cancelAllForUser(
            userId: Long
        ) = Unit
    }
}
