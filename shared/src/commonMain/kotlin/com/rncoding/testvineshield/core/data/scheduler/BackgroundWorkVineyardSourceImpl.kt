package com.rncoding.testvineshield.core.data.scheduler

import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.scheduler.BackgroundWorkVineyardSource
import kotlinx.coroutines.flow.first

class BackgroundWorkVineyardSourceImpl(
    private val vineyardRepository: VineyardRepository
) : BackgroundWorkVineyardSource {

    override suspend fun getVineyardIds(userId: Long): List<Long> {
        return vineyardRepository
            .observeVineyards(userId)
            .first()
            .map { it.vineyardId }
    }
}