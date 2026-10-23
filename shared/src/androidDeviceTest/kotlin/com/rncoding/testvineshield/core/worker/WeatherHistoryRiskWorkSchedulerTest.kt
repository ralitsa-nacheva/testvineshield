
package com.rncoding.testvineshield.core.worker

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class WeatherHistoryRiskWorkSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WeatherHistoryRiskWorkScheduler

    private val userId = 10L
    private val vineyardId = 100L

    @Before
    fun setUp() {
        context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val configuration = Configuration.Builder()
            .setWorkerFactory(TestWorkerFactory())
            .build()

        // Use an isolated WorkManager test environment.
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            configuration
        )

        workManager = WorkManager.getInstance(context)

        scheduler = WeatherHistoryRiskWorkScheduler(
            workManager = workManager
        )
    }

    @After
    fun tearDown() {
        // Clear work created by the current test.
        workManager.cancelAllWork()
            .result
            .get(10, TimeUnit.SECONDS)
    }

    @Test
    fun schedulingVineyardCreatesPeriodicWork() {
        scheduler.schedule(userId, vineyardId)

        val work = getWork(userId, vineyardId)

        assertEquals(1, work.size)
        assertTrue(work.single().tags.contains(userTag(userId)))
        assertTrue(
            work.single().tags.contains(
                vineyardTag(userId, vineyardId)
            )
        )
    }

    @Test
    fun schedulingSameVineyardTwiceKeepsExistingWork() {
        scheduler.schedule(userId, vineyardId)

        val original = getWork(userId, vineyardId).single()

        scheduler.schedule(userId, vineyardId)

        val afterReschedule = getWork(userId, vineyardId)

        assertEquals(1, afterReschedule.size)
        assertEquals(original.id, afterReschedule.single().id)
    }

    @Test
    fun differentVineyardsHaveIndependentWork() {
        scheduler.schedule(userId, 100L)
        scheduler.schedule(userId, 200L)

        val first = getWork(userId, 100L)
        val second = getWork(userId, 200L)

        assertEquals(1, first.size)
        assertEquals(1, second.size)

        assertTrue(first.single().id != second.single().id)
    }

    @Test
    fun sameVineyardIdForDifferentUsersHasIndependentWork() {
        scheduler.schedule(10L, vineyardId)
        scheduler.schedule(20L, vineyardId)

        val first = getWork(10L, vineyardId)
        val second = getWork(20L, vineyardId)

        assertEquals(1, first.size)
        assertEquals(1, second.size)

        assertTrue(first.single().id != second.single().id)
    }

    @Test
    fun cancellingOneVineyardDoesNotCancelAnother() {
        scheduler.schedule(userId, 100L)
        scheduler.schedule(userId, 200L)

        scheduler.cancel(userId, 100L)

        awaitCancelled(userId, 100L)

        val first = getWork(userId, 100L).single()
        val second = getWork(userId, 200L).single()

        assertEquals(WorkInfo.State.CANCELLED, first.state)
        assertTrue(second.state != WorkInfo.State.CANCELLED)
    }

    @Test
    fun cancellingUserWorkDoesNotCancelOtherUsersWork() {
        scheduler.schedule(10L, 100L)
        scheduler.schedule(10L, 200L)
        scheduler.schedule(20L, 300L)

        scheduler.cancelAllForUser(10L)

        awaitCancelled(10L, 100L)
        awaitCancelled(10L, 200L)

        val first = getWork(10L, 100L).single()
        val second = getWork(10L, 200L).single()
        val otherUser = getWork(20L, 300L).single()

        assertEquals(WorkInfo.State.CANCELLED, first.state)
        assertEquals(WorkInfo.State.CANCELLED, second.state)

        assertTrue(otherUser.state != WorkInfo.State.CANCELLED)
    }

    @Test
    fun invalidIdsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            scheduler.schedule(0L, vineyardId)
        }

        assertFailsWith<IllegalArgumentException> {
            scheduler.schedule(userId, -1L)
        }

        assertFailsWith<IllegalArgumentException> {
            scheduler.cancel(0L, vineyardId)
        }

        assertFailsWith<IllegalArgumentException> {
            scheduler.cancelAllForUser(0L)
        }
    }

    private fun getWork(
        userId: Long,
        vineyardId: Long
    ): List<WorkInfo> {
        return workManager
            .getWorkInfosForUniqueWork(
                uniqueWorkName(userId, vineyardId)
            )
            .get(10, TimeUnit.SECONDS)
    }

    private fun awaitCancelled(
        userId: Long,
        vineyardId: Long
    ) {
        // Cancellation is asynchronous. Poll briefly rather
        // than assuming the state changes immediately.
        repeat(50) {
            val work = getWork(userId, vineyardId)

            if (
                work.isNotEmpty() &&
                work.all { it.state == WorkInfo.State.CANCELLED }
            ) {
                return
            }

            Thread.sleep(100)
        }

        assertTrue(
            getWork(userId, vineyardId).all {
                it.state == WorkInfo.State.CANCELLED
            },
            "Expected work to be cancelled"
        )
    }

    private fun uniqueWorkName(
        userId: Long,
        vineyardId: Long
    ): String =
        "weather_history_risk_user_${userId}_vineyard_$vineyardId"

    private fun userTag(userId: Long): String =
        "weather_history_risk_user_$userId"

    private fun vineyardTag(
        userId: Long,
        vineyardId: Long
    ): String =
        "weather_history_risk_vineyard_${userId}_$vineyardId"

    /**
     * Prevents the test from invoking the real worker.
     *
     * The scheduler tests only work registration and
     * cancellation, not weather downloading or risk evaluation.
     */
    private class TestWorkerFactory : WorkerFactory() {

        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            return if (
                workerClassName ==
                WeatherHistoryRiskWorker::class.java.name
            ) {
                TestWorker(appContext, workerParameters)
            } else {
                null
            }
        }
    }

    private class TestWorker(
        context: Context,
        parameters: WorkerParameters
    ) : Worker(context, parameters) {

        override fun doWork(): Result {
            return Result.success()
        }
    }
}
