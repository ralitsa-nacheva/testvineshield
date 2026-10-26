package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.ActivityDeletionOutcome
import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun observeActivities(vineyardId: Long): Flow<List<ActivityDomainModel>>
    suspend fun addActivity(activity: ActivityDomainModel): Long?

    suspend fun belongsToVineyard(
        vineyardId: Long,
        blockId: Long
    ): Boolean


    suspend fun getActivity(
        vineyardId: Long,
        activityId: Long
    ): ActivityDomainModel?

    suspend fun updateActivity(
        vineyardId: Long,
        activity: ActivityDomainModel
    ): Boolean

    suspend fun deleteActivity(
        vineyardId: Long,
        activityId: Long
    ): ActivityDeletionOutcome

}