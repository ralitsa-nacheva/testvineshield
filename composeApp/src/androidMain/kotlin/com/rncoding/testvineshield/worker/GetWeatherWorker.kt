package com.rncoding.testvineshield.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.repository.WeatherRepository
import com.rncoding.testvineshield.vineyard_details.domain.Vineyard
import kotlin.text.iterator


class GetWeatherWorker (appContext: Context,
                        params: WorkerParameters,
                        private val vineyardRepository: VineyardRepository,
                        private val weatherRepository: WeatherRepository,
                        private val diseaseRepository: DiseaseRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        return try {

            val vineyards = vineyardRepository.getAllVineyardIds()

            for (vineyard in vineyards) {

                val priority = calculatePriority(vineyard)

                when (priority) {

                    SyncPriority.HIGH -> {
                        updateVineyard(vineyard)
                    }

                    SyncPriority.MEDIUM -> {
                        if (shouldUpdateMedium(vineyard)) {
                            updateVineyard(vineyard)
                        }
                    }

                    SyncPriority.LOW -> {
                        if (shouldUpdateLow(vineyard)) {
                            updateVineyard(vineyard)
                        }
                    }
                }
            }

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }

    fun calculatePriority(vineyard: VineyardEntity): SyncPriority {

        val now = System.currentTimeMillis()

        return when {
            now - vineyard.lastOpenedAt < 1 * 60 * 60 * 1000 -> SyncPriority.HIGH
            now - vineyard.lastOpenedAt < 24 * 60 * 60 * 1000 -> SyncPriority.MEDIUM
            else -> SyncPriority.LOW
        }
    }

    private suspend fun updateVineyardWeather(vineyard: VineyardDomainModel) {

        weatherRepository.refreshWeather(
            vineyardId = vineyard.vineyardId,
            latitude = vineyard.latitude,
            longitude = vineyard.longitude
        )

        diseaseRepository.generateAlerts(
            vineyardId = vineyard.id,
            diseaseId = 1
        )
    }
}