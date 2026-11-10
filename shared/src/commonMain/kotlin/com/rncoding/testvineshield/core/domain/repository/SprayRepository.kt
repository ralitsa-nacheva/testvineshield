package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.SprayRecord
import com.rncoding.testvineshield.core.domain.datamodels.SprayWriteOutcome

interface SprayRepository {
    suspend fun get(vineyardId: Long, activityId: Long): SprayRecord?
    suspend fun save(vineyardId: Long, record: SprayRecord): SprayWriteOutcome
    suspend fun delete(vineyardId: Long, activityId: Long, expectedRevision: Long, generation: String): SprayWriteOutcome
}
