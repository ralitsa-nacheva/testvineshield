package com.rncoding.testvineshield.core.domain.scheduler

interface BackgroundWorkVineyardSource {

    suspend fun getVineyardIds(userId: Long): List<Long>
}