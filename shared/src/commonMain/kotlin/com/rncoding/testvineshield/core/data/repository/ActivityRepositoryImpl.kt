
package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.ActivityDao
import com.rncoding.testvineshield.core.data.local.database.daos.BlockDao
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityPriority
import com.rncoding.testvineshield.core.domain.datamodels.enums.ActivityStatus
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActivityRepositoryImpl(
    private val activityDao: ActivityDao,
    private val blockDao: BlockDao
) : ActivityRepository {

    override fun observeActivities(
        vineyardId: Long
    ): Flow<List<ActivityDomainModel>> {
        require(vineyardId > 0L)

        return activityDao.observeActivitiesByVineyard(vineyardId)
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun addActivity(
        activity: ActivityDomainModel
    ) {
        require(activity.vineyardId > 0L)
        require(activity.activityId >= 0L)
        require(activity.blockId == null || activity.blockId > 0L)

        activityDao.upsertActivity(activity.toEntity())
    }

    private fun ActivityEntity.toDomain(): ActivityDomainModel =
        ActivityDomainModel(
            activityId = activityId,
            vineyardId = vineyardId,
            blockId = blockId,
            activityType = activityType,
            activityDescription = activityDescription,
            season = season,
            phenologicalStage = phenologicalStage.dbValue,
            priority = priority.dbValue,
            status = status.dbValue,
            plannedAt = plannedAt,
            completedAt = completedAt,
            tool = tool,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
            treatmentProduct = treatmentProduct,
            treatmentProductQuantity = treatmentProductQuantity,
            treatmentProductUnits = treatmentProductUnits,
            waterQuantity = waterQuantity,
            treatmentProductRate = treatmentProductRate
        )

    private fun ActivityDomainModel.toEntity(): ActivityEntity =
        ActivityEntity(
            activityId = activityId,
            vineyardId = vineyardId,
            blockId = blockId,
            activityType = activityType,
            activityDescription = activityDescription,
            season = season,
            phenologicalStage =
                PhenologicalStage.fromDbValue(phenologicalStage),
            priority = ActivityPriority.fromDbValue(priority),
            status = ActivityStatus.fromDbValue(status),
            plannedAt = plannedAt,
            completedAt = completedAt,
            tool = tool,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt,
            treatmentProduct = treatmentProduct,
            treatmentProductQuantity = treatmentProductQuantity,
            treatmentProductUnits = treatmentProductUnits,
            waterQuantity = waterQuantity,
            treatmentProductRate = treatmentProductRate
        )

    override suspend fun belongsToVineyard(
        vineyardId: Long,
        blockId: Long
    ): Boolean {
        if (vineyardId <= 0L || blockId <= 0L) {
            return false
        }

        return blockDao.getBlockById(
            vineyardId = vineyardId,
            blockId = blockId
        ) != null
    }


    override suspend fun getActivity(
        vineyardId: Long,
        activityId: Long
    ): ActivityDomainModel? {
        require(vineyardId > 0L) {
            "Vineyard ID must be positive."
        }

        require(activityId > 0L) {
            "Activity ID must be positive."
        }

        return activityDao.getActivityById(
            vineyardId = vineyardId,
            activityId = activityId
        )?.toDomain()
    }


    override suspend fun updateActivity(
        vineyardId: Long,
        activity: ActivityDomainModel
    ): Boolean {
        require(vineyardId > 0L) {
            "Vineyard ID must be positive."
        }

        require(activity.activityId > 0L) {
            "Activity ID must be positive."
        }

        require(activity.vineyardId == vineyardId) {
            "Activity vineyard does not match the requested vineyard."
        }

        return activityDao.updateActivityForVineyard(
            vineyardId = vineyardId,
            activity = activity.toEntity()
        )
    }


    override suspend fun deleteActivity(
        vineyardId: Long,
        activityId: Long
    ): Boolean {
        require(vineyardId > 0L) {
            "Vineyard ID must be positive."
        }

        require(activityId > 0L) {
            "Activity ID must be positive."
        }

        return activityDao.deleteActivityForVineyard(
            vineyardId = vineyardId,
            activityId = activityId
        ) == 1
    }


}
