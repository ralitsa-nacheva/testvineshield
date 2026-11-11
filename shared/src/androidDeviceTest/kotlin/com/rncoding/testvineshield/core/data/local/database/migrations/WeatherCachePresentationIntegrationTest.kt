package com.rncoding.testvineshield.core.data.local.database.migrations

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.local.database.*
import com.rncoding.testvineshield.core.data.local.database.entities.*
import com.rncoding.testvineshield.core.data.repository.WeatherForecastRepositoryImpl
import com.rncoding.testvineshield.core.data.disease_risk.DiseaseRiskMonitoringConfigurationRepositoryImpl
import com.rncoding.testvineshield.core.data.local.mappers.WeatherForecastMapper
import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationCoordinator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class WeatherCachePresentationIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun open(name: String): VineshieldDatabase {
        context.getDatabasePath(name).parentFile?.mkdirs()
        val connection = BundledSQLiteDriver().open(context.getDatabasePath(name).absolutePath)
        try { SchemaV2Fixture.create(connection) } finally { connection.close() }
        return buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context,name))
    }
    @Test fun allConfigurationStreamIncludesDisabledRecordsInStableScopeOrder(): Unit = runBlocking {
        val name = "r06a-config-${UUID.randomUUID()}.db"; val db = open(name)
        try {
            db.diseaseDao().insertDisease(DiseaseEntity(70,"test","Test disease","","","",""))
            val dao = db.diseaseRiskMonitoringConfigurationDao()
            dao.upsertConfiguration(DiseaseRiskMonitoringConfigurationEntity(10,20,70,"POWDERY_MILDEW_UC_DAVIS","2026-11-01",false))
            dao.upsertConfiguration(DiseaseRiskMonitoringConfigurationEntity(10,20,70,"BOTRYTIS","2026-10-01",true))
            val repo = DiseaseRiskMonitoringConfigurationRepositoryImpl(dao,DiseaseRiskEvaluationCoordinator())
            val rows = repo.observeConfigurationsForVineyard(10).first()
            assertEquals(listOf("BOTRYTIS","POWDERY_MILDEW_UC_DAVIS"), rows.map { it.modelType.name })
            assertEquals(listOf(true,false), rows.map { it.enabled })
            assertEquals(1, repo.getEnabledConfigurationsForVineyard(10).size)
            assertTrue(repo.observeConfigurationsForVineyard(11).first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun forecastMetadataSurvivesMappingAndObservationNeverUsesNetwork(): Unit = runBlocking {
        val name = "r06a-cache-${UUID.randomUUID()}.db"; val db = open(name)
        try {
            val noNetwork = object : WeatherRemoteDataSource {
                override suspend fun fetchWeather(latitude: Double, longitude: Double, timeZone: String): WeatherDto = error("Unexpected history request")
                override suspend fun fetchWeatherForecast(latitude: Double, longitude: Double, timeZone: String): WeatherDto = error("Unexpected forecast request")
            }
            val first = WeatherForecastEntity(0,10,2_000_000_000_000L,18.0,60.0,10.0,20.0,4.0,0.0,0.0,0.0,0.0,0.0,0,2000.0,10000.0,12.0,10.0,0.3,0.3,true,1_999_999_000_000L)
            val request=assertNotNull(db.weatherCacheDao().begin(10,42.1,24.7,"Europe/Sofia","forecast",first.forecastCreatedAt))
            assertTrue(db.weatherCacheDao().completeForecast(request,listOf(first,first.copy(timestamp=first.timestamp+3_600_000L)),first.forecastCreatedAt))
            val repo = WeatherForecastRepositoryImpl(noNetwork, db.vineyardDao(), WeatherForecastMapper(),db.weatherCacheDao())
            val rows = repo.observeCachedForecast(10).first()
            assertEquals(2, rows.size); assertEquals(first.timestamp,rows.first().weather.timestamp)
            assertTrue(rows.all { it.fetchedAt == first.forecastCreatedAt && it.weather.vineyardId == 10L })
            assertEquals(repo.observeForecast(10).first(),rows.map { it.weather })
            assertTrue(repo.observeCachedForecast(11).first().isEmpty())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
