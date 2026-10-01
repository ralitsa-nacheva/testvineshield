package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardSummary
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import kotlinx.coroutines.flow.Flow

interface VineyardRepository {

    fun observeVineyards(
        userId: Long
    ): Flow<List<VineyardDomainModel>>

    fun observeVineyardSummaries(
        userId: Long
    ): Flow<List<VineyardSummary>>

    fun observeVineyard(
        userId: Long,
        vineyardId: Long
    ): Flow<VineyardDomainModel?>

    suspend fun getVineyard(
        userId: Long,
        vineyardId: Long
    ): Result<VineyardDomainModel, AppError>

    suspend fun createVineyard(
        vineyard: VineyardDomainModel
    ): Result<Long, AppError>

    suspend fun updateVineyard(
        vineyard: VineyardDomainModel
    ): Result<Unit, AppError>

    suspend fun deleteVineyard(
        userId: Long,
        vineyardId: Long
    ): Result<Unit, AppError>

    suspend fun reorderVineyards(
        userId: Long,
        vineyardIds: List<Long>
    ): Result<Unit, AppError>

    suspend fun getNextSortOrder(
        userId: Long
    ):  Result<Int, AppError>
}