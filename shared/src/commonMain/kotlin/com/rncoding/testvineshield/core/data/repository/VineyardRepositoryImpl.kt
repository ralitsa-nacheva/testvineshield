package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.local.database.relations.VineyardWithActivities
import com.rncoding.testvineshield.core.data.local.database.relations.VineyardWithBlocks
import com.rncoding.testvineshield.core.data.local.database.relations.VineyardWithCalculations
import com.rncoding.testvineshield.core.data.local.mappers.VineyardMapper
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import kotlinx.coroutines.flow.Flow

class VineyardRepositoryImpl(
    private val vineyardDao: VineyardDao,
    private val vineyardMapper: VineyardMapper
): VineyardRepository {
    override suspend fun upsertVineyard(domainVineyard: VineyardDomainModel) {
        vineyardDao.upsertVineyard(vineyardMapper.domainToEntity(domainVineyard))
    }

    override suspend fun getAllVineyards(): List<VineyardDomainModel> {
        return vineyardDao.getAllVineyards()
            .map{entity -> vineyardMapper.entityToDomain(entity)}
    }

    override suspend fun getAllVineyardsForUser(userId: Long): Flow<List<VineyardDomainModel>> {
        TODO("Not yet implemented")
    }

    override suspend fun getVineyardById(vineyardId: Long): Flow<VineyardDomainModel> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllVineyardIds(): List<Long> {
         return vineyardDao.getAllVineyardIds()
             }

    override suspend fun getVineyardByCity(city: String): List<VineyardDomainModel> {
        return vineyardDao.getVineyardByCity(city)
            .map{entity -> vineyardMapper.entityToDomain(entity)}
    }

    override suspend fun getVineyardByName(name: String): List<VineyardDomainModel> {
        return vineyardDao.getVineyardByName(name)
            .map{entity -> vineyardMapper.entityToDomain(entity)}
    }

    override suspend fun deleteAllVineyards() {
        vineyardDao.deleteAllVineyards()
    }

    override suspend fun deleteVineyardById(vineyardId: Long) {
        vineyardDao.deleteVineyardById(vineyardId)
    }

    override suspend fun getVineyardWithBlocks(vineyardId: Long): VineyardWithBlocks {
        return vineyardDao.getVineyardWithBlocks(vineyardId)
    }

    override suspend fun  getVineyardWithCalculations(vineyardId: Long): VineyardWithCalculations {
        return vineyardDao.getVineyardWithCalculations(vineyardId)
    }

    override suspend fun getVineyardWithActivities(vineyardId: Long): VineyardWithActivities {
        return vineyardDao.getVineyardWithActivities(vineyardId)
    }


}