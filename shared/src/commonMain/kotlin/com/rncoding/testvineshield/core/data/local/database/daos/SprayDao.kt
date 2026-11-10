package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.*
import kotlin.random.Random
import com.rncoding.testvineshield.core.data.local.database.entities.*
import com.rncoding.testvineshield.core.domain.datamodels.SprayWriteOutcome

data class SprayRows(val application: SprayApplicationEntity, val products: List<SprayProductEntity>)

@Dao
abstract class SprayDao {
    @Query("SELECT EXISTS(SELECT 1 FROM activity WHERE vineyard_id=:vineyardId AND activity_id=:activityId)")
    abstract suspend fun activityExists(vineyardId: Long, activityId: Long): Boolean

    @Query("SELECT s.* FROM spray_application s JOIN activity a ON a.activity_id=s.activity_id WHERE a.vineyard_id=:vineyardId AND s.activity_id=:activityId")
    abstract suspend fun header(vineyardId: Long, activityId: Long): SprayApplicationEntity?

    @Query("SELECT * FROM spray_product WHERE activity_id=:activityId ORDER BY line_position, product_line_id")
    abstract suspend fun lines(activityId: Long): List<SprayProductEntity>

    @Upsert protected abstract suspend fun upsertHeader(application: SprayApplicationEntity)
    @Insert protected abstract suspend fun insertLines(products: List<SprayProductEntity>)
    @Query("DELETE FROM spray_product WHERE activity_id=:activityId")
    protected abstract suspend fun clearLines(activityId: Long)
    @Query("DELETE FROM spray_application WHERE activity_id=:activityId")
    protected abstract suspend fun removeHeader(activityId: Long)

    @Transaction
    open suspend fun get(vineyardId: Long, activityId: Long): SprayRows? {
        val application = header(vineyardId, activityId) ?: return null
        return SprayRows(application, lines(activityId))
    }

    @Transaction
    open suspend fun replace(vineyardId: Long, application: SprayApplicationEntity, products: List<SprayProductEntity>): SprayWriteOutcome {
        if (!activityExists(vineyardId, application.activityId)) return SprayWriteOutcome.ACTIVITY_NOT_FOUND
        val old = header(vineyardId, application.activityId)
        if (application.revision != (old?.revision ?: 0) || application.generation != (old?.generation ?: "")) return SprayWriteOutcome.CONFLICT
        val existingIds = lines(application.activityId).map { it.lineId }.toSet()
        val suppliedIds = products.map { it.lineId }.filter { it > 0 }
        if (products.isEmpty() || products.size > 20 || suppliedIds.distinct().size != suppliedIds.size ||
            suppliedIds.any { it !in existingIds } || products.withIndex().any { (index, line) ->
                line.lineId < 0 || line.activityId != application.activityId || line.position != index
            }) return SprayWriteOutcome.INVALID_LINE_ID
        val generation = old?.generation ?: Random.nextBytes(16).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        upsertHeader(application.copy(revision = (old?.revision ?: 0) + 1, generation = generation))
        // Preserve known line IDs; generated IDs are used only for newly added lines.
        // Deleting/reinserting is safe because no table references product-line IDs.
        clearLines(application.activityId)
        insertLines(products)
        return SprayWriteOutcome.SAVED
    }

    @Transaction
    open suspend fun delete(vineyardId: Long, activityId: Long, expectedRevision: Long, generation: String): SprayWriteOutcome {
        if (!activityExists(vineyardId, activityId)) return SprayWriteOutcome.ACTIVITY_NOT_FOUND
        val old = header(vineyardId, activityId) ?: return SprayWriteOutcome.CONFLICT
        if (old.revision != expectedRevision || old.generation != generation) return SprayWriteOutcome.CONFLICT
        removeHeader(activityId)
        return SprayWriteOutcome.SAVED
    }
}
