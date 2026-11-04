package com.rncoding.testvineshield.core.data.scheduler

import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler
import com.rncoding.testvineshield.core.domain.scheduler.ForegroundWeatherOutcome
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

/** All mutable state is confined to the injected serial dispatcher (Main on iOS).
 * Registrations are rebuilt by authenticated session restore after process death.
 * This is foreground work, not an operating-system background task.
 */
class ForegroundWeatherHistoryRiskScheduler(
    private val scope: CoroutineScope,
    private val execute: suspend (Long, Long) -> ForegroundWeatherOutcome,
    private val intervalMillis: Long = 60 * 60 * 1000L,
    private val retryMillis: Long = 60 * 1000L,
    private val nowMillis: () -> Long = monotonicClock()
) : WeatherHistoryRiskScheduler {
    init { require(intervalMillis > 0); require(retryMillis > 0) }
    private data class Key(val userId: Long, val vineyardId: Long)
    private class Registration(var job: Job? = null, var nextDue: Long = 0, var retries: Int = 0)
    private val registrations = mutableMapOf<Key, Registration>()
    private val executionLock = Mutex()
    private var active = false

    override fun schedule(userId: Long, vineyardId: Long) {
        require(userId > 0 && vineyardId > 0)
        scope.launch {
            val key = Key(userId, vineyardId)
            val registration = registrations.getOrPut(key) { Registration() }
            if (active) start(key, registration)
        }
    }

    override fun cancel(userId: Long, vineyardId: Long) {
        scope.launch { registrations.remove(Key(userId, vineyardId))?.job?.cancel() }
    }

    override fun cancelAllForUser(userId: Long) {
        scope.launch {
            registrations.keys.filter { it.userId == userId }.forEach {
                registrations.remove(it)?.job?.cancel()
            }
        }
    }

    fun setForeground(isForeground: Boolean) {
        scope.launch {
            if (active == isForeground) return@launch
            active = isForeground
            if (active) registrations.toList().forEach { (key, value) -> start(key, value) }
            else registrations.values.forEach { it.job?.cancel(); it.job = null }
        }
    }

    private fun start(key: Key, registration: Registration) {
        if (registration.job?.isActive == true) return
        val job = scope.launch(start = CoroutineStart.LAZY) {
            while (isActive && active && registrations[key] === registration) {
                delay((registration.nextDue - nowMillis()).coerceAtLeast(0))
                val outcome = try {
                    executionLock.withLock {
                        ensureActive()
                        execute(key.userId, key.vineyardId)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Unexpected/startup failures use the normal interval, never a tight loop.
                    ForegroundWeatherOutcome.FAILED
                }
                ensureActive()
                if (outcome == ForegroundWeatherOutcome.REMOVE) {
                    registrations.remove(key)
                    return@launch
                }
                val wait = if (outcome == ForegroundWeatherOutcome.RETRY && registration.retries < 3) {
                    registration.retries++
                    retryMillis
                } else {
                    registration.retries = 0
                    intervalMillis
                }
                registration.nextDue = nowMillis() + wait
            }
        }
        registration.job = job
        job.start()
    }
    private companion object {
        fun monotonicClock(): () -> Long {
            val origin = TimeSource.Monotonic.markNow()
            return { origin.elapsedNow().inWholeMilliseconds }
        }
    }
}

