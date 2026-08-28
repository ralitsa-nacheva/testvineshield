package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.database.relations.VineyardWithActivities
import com.rncoding.testvineshield.core.data.local.database.relations.VineyardWithBlocks
import com.rncoding.testvineshield.core.data.local.database.relations.VineyardWithCalculations
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import kotlinx.coroutines.flow.Flow


interface VineyardRepository {
    suspend fun upsertVineyard(domainVineyard: VineyardDomainModel)

    suspend fun getAllVineyards(): List<VineyardDomainModel>

    suspend fun getAllVineyardsForUser(userId: Long): Flow<List<VineyardDomainModel>>

    suspend fun getVineyardById(vineyardId: Long): Flow<VineyardDomainModel>

    suspend fun getAllVineyardIds(): List<VineyardDomainModel>

    suspend fun getVineyardByCity(city: String): List<VineyardDomainModel>

    suspend fun getVineyardByName(name: String): List<VineyardDomainModel>

    suspend fun deleteAllVineyards()

    suspend fun deleteVineyardById(vineyardId: Long)

    suspend fun getVineyardWithBlocks(vineyardId: Long): VineyardWithBlocks

    suspend fun  getVineyardWithCalculations(vineyardId: Long): VineyardWithCalculations

    suspend fun getVineyardWithActivities(vineyardId: Long): VineyardWithActivities

}