package com.rncoding.testvineshield.worker

import androidx.work.ListenableWorker
import com.rncoding.testvineshield.core.domain.repository.VineyardRepository
import com.rncoding.testvineshield.core.domain.repository.WeatherRepository


class WeatherWorkerFactory {
}private val vineyardRepository: VineyardRepository,
private val weatherRepository: WeatherRepository,
private val diseaseRepository: DiseaseRepository
) : WorkerFactory() {

    override fun createWorker(...): ListenableWorker? {

        return when (workerClassName) {

            GetWeatherWorker::class.java.name ->
                GetWeatherWorker(
                    appContext,
                    params,
                    vineyardRepository,
                    weatherRepository,
                    diseaseRepository
                )

            else -> null
        }
    }
}