package com.rncoding.testvineshield.core.data.startup

import com.rncoding.testvineshield.core.data.local.database.seed.DiseaseCatalogueSeeder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DefaultAppInitializerTest {
    private val seeder = mockk<DiseaseCatalogueSeeder>()
    private val initializer = DefaultAppInitializer(seeder)

    @Test
    fun successfulInitializationIsReused(): Unit = runBlocking {
        coEvery { seeder.seed() } returns Unit
        initializer.ensureInitialized()
        initializer.ensureInitialized()
        coVerify(exactly = 1) { seeder.seed() }
    }

    @Test
    fun concurrentCallersWaitForOneSuccessfulInitialization(): Unit = runBlocking {
        val release = CompletableDeferred<Unit>()
        var calls = 0
        coEvery { seeder.seed() } coAnswers {
            calls++
            release.await()
        }
        val callers = List(20) {
            async(start = CoroutineStart.UNDISPATCHED) { initializer.ensureInitialized() }
        }
        assertEquals(1, calls)
        assertFalse(callers.any { it.isCompleted })
        release.complete(Unit)
        callers.awaitAll()
        assertEquals(1, calls)
    }

    @Test
    fun failureDoesNotBecomePermanentReadiness(): Unit = runBlocking {
        var calls = 0
        coEvery { seeder.seed() } coAnswers {
            calls++
            if (calls == 1) throw IllegalStateException("Conflict")
        }
        assertFailsWith<IllegalStateException> { initializer.ensureInitialized() }
        initializer.ensureInitialized()
        initializer.ensureInitialized()
        assertEquals(2, calls)
    }

    @Test
    fun cancellingInitializingCallerReleasesGateForAnotherAttempt(): Unit = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        coEvery { seeder.seed() } coAnswers {
            calls++
            if (calls == 1) {
                entered.complete(Unit)
                release.await()
            }
        }
        val first = launch { initializer.ensureInitialized() }
        entered.await()
        first.cancelAndJoin()
        initializer.ensureInitialized()
        assertEquals(2, calls)
    }

    @Test
    fun cancellingWaitingCallerDoesNotCancelActiveInitialization(): Unit = runBlocking {
        val release = CompletableDeferred<Unit>()
        coEvery { seeder.seed() } coAnswers { release.await() }
        val first = async(start = CoroutineStart.UNDISPATCHED) { initializer.ensureInitialized() }
        val waiter = launch(start = CoroutineStart.UNDISPATCHED) { initializer.ensureInitialized() }
        waiter.cancelAndJoin()
        assertFalse(first.isCompleted)
        release.complete(Unit)
        first.await()
        initializer.ensureInitialized()
        coVerify(exactly = 1) { seeder.seed() }
    }

    @Test
    fun cancellationExceptionIsPropagatedAndNotCached(): Unit = runBlocking {
        coEvery { seeder.seed() } throws CancellationException("Stopped")
        assertFailsWith<CancellationException> { initializer.ensureInitialized() }
        coEvery { seeder.seed() } returns Unit
        initializer.ensureInitialized()
        coVerify(exactly = 2) { seeder.seed() }
    }
}
