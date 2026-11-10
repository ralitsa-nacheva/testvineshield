package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.SprayDao
import com.rncoding.testvineshield.core.data.local.database.entities.*
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.domain.repository.SprayRepository

class SprayRepositoryImpl(private val dao: SprayDao) : SprayRepository {
    override suspend fun get(vineyardId: Long, activityId: Long): SprayRecord? {
        val rows = dao.get(vineyardId, activityId) ?: return null
        val app = rows.application
        return SprayRecord(app.activityId, app.applicationDate, app.localTime, app.timeZone,
            app.treatedArea, app.areaUnit?.let(AreaUnit::fromCode), app.waterQuantity,
            app.waterUnit?.let(SprayQuantityUnit::fromCode), app.notes, app.revision,
            rows.products.map { SprayProductLine(it.lineId, it.productName, it.quantity, it.quantityUnit?.let(SprayQuantityUnit::fromCode), it.labelReference, rate(it)) }, app.generation)
    }
    private fun rate(line: SprayProductEntity): SprayRate? {
        if (line.rateQuantity == null && line.rateProductUnit == null && line.rateBasisQuantity == null && line.rateBasisUnit == null) return null
        // Partially stored rates indicate invalid storage, not an unknown rate.
        return SprayRate(checkNotNull(line.rateQuantity), SprayQuantityUnit.fromCode(checkNotNull(line.rateProductUnit)),
            checkNotNull(line.rateBasisQuantity), SprayRateBasis.fromCode(checkNotNull(line.rateBasisUnit)))
    }
    override suspend fun save(vineyardId: Long, record: SprayRecord): SprayWriteOutcome = dao.replace(vineyardId,
        SprayApplicationEntity(record.activityId, record.applicationDate, record.localTime, record.timeZone,
            record.treatedArea, record.areaUnit?.code, record.waterQuantity, record.waterUnit?.code, record.notes, record.revision, record.generation.orEmpty()),
        record.products.mapIndexed { index, line -> SprayProductEntity(line.lineId, record.activityId, index, line.productName, line.quantity, line.unit?.code, line.labelReference, line.rate?.quantity, line.rate?.productUnit?.code, line.rate?.basisQuantity, line.rate?.basis?.code) })
    override suspend fun delete(vineyardId: Long, activityId: Long, expectedRevision: Long, generation: String): SprayWriteOutcome =
        dao.delete(vineyardId, activityId, expectedRevision, generation)
}
