package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.ActivityDomainModel
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun observeActivities(vineyardId: Long): Flow<List<ActivityDomainModel>>
    suspend fun addActivity(activity: ActivityDomainModel)
}