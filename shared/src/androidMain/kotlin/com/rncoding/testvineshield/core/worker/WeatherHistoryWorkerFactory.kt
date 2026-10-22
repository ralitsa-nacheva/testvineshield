
package com.rncoding.testvineshield.core.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.rncoding.testvineshield.core.domain.usecases.weather.AuthorizeWeatherHistoryBackgroundWorkUseCase
import com.rncoding.testvineshield.core.domain.usecases.weather.RefreshWeatherHistoryAndEvaluateRiskUseCase
import org.koin.core.Koin

class WeatherHistoryWorkerFactory(
    private val koin: Koin
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        if (workerClassName != WeatherHistoryRiskWorker::class.java.name) {
            return null
        }

        return WeatherHistoryRiskWorker(
            appContext = appContext,
            workerParameters = workerParameters,
            authorizeBackgroundWork = koin.get<
                    AuthorizeWeatherHistoryBackgroundWorkUseCase
                    >(),
            refreshWeatherHistoryAndEvaluateRisk = koin.get<
                    RefreshWeatherHistoryAndEvaluateRiskUseCase
                    >()
        )
    }
}
