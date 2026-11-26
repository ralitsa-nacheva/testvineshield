package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertInboxItem
import kotlinx.coroutines.flow.Flow

interface DiseaseAlertRepository {
    fun observeOwnedInbox(ownerId: Long): Flow<List<DiseaseAlertInboxItem>>
    suspend fun acknowledgeOwned(ownerId: Long, expected: DiseaseAlertInboxItem): Boolean
}
