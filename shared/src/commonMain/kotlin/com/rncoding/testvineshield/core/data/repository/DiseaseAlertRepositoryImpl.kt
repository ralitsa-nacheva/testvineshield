package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseAlertDao
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertInboxItem
import com.rncoding.testvineshield.core.domain.repository.DiseaseAlertRepository
import kotlinx.coroutines.flow.map

class DiseaseAlertRepositoryImpl(private val dao: DiseaseAlertDao) : DiseaseAlertRepository {
    override fun observeOwnedInbox(ownerId: Long) = dao.observeOwnedInbox(ownerId).map { rows ->
        rows.map { row -> DiseaseAlertInboxItem(row.alertId, row.vineyardId, row.vineyardName, row.blockId, row.blockName, row.diseaseId, row.diseaseCode, row.diseaseName, row.createdAt, row.message, row.status, row.severity, row.stage) }
    }
    override suspend fun acknowledgeOwned(ownerId: Long, expected: DiseaseAlertInboxItem): Boolean =
        dao.acknowledgeOwned(ownerId, expected.vineyardId, expected.alertId, expected.diseaseId,
            expected.blockId, expected.createdAt, expected.message, expected.severity, expected.stage) == 1
}
