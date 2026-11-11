package com.rncoding.testvineshield.core.data.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.data.local.database.*
import com.rncoding.testvineshield.core.data.local.database.entities.*
import com.rncoding.testvineshield.core.data.local.mappers.WeatherForecastMapper
import com.rncoding.testvineshield.core.data.remote.WeatherResponseFixtures
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class WeatherForecastValidationIntegrationTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val fetchedAt=1_800_000_000_000L
    private fun open(name: String)=buildVineshieldDatabase(Room.databaseBuilder<VineshieldDatabase>(context,name))
    private class Remote(var response: WeatherDto=WeatherResponseFixtures.response()) : WeatherRemoteDataSource {
        override suspend fun fetchWeather(latitude: Double,longitude: Double,timeZone: String): WeatherDto=error("Unexpected history fetch")
        override suspend fun fetchWeatherForecast(latitude: Double,longitude: Double,timeZone: String)=response
    }
    private suspend fun seed(db: VineshieldDatabase): Pair<Long,Long> {
        val owner=db.userDao().insertUser(UserEntity(email="forecast@example.com",passwordHash="hash",passwordSalt="salt"))
        val date=LocalDate(2026,11,10)
        suspend fun vineyard(name: String)=db.vineyardDao().insertVineyard(VineyardEntity(userId=owner,name=name,size=1.0,country="BG",city="Sofia",latitude=42.7,longitude=23.3,timeZone="Europe/Sofia",elevation=null,sortOrder=0,createdAt=date,updatedAt=date))
        val first=vineyard("First"); val other=vineyard("Other")
        val mapper=WeatherForecastMapper()
        db.weatherForecastDao().replaceForecast(first,mapper.weatherDtoToEntities(WeatherResponseFixtures.response(2),first,fetchedAt-1000,fetchedAt))
        db.weatherForecastDao().replaceForecast(other,mapper.weatherDtoToEntities(WeatherResponseFixtures.response(2),other,fetchedAt-1000,fetchedAt))
        return first to other
    }
    @Test fun malformedResponsesPreserveTheExactStoredCache(): Unit=runBlocking {
        val name="r06b1-invalid-${UUID.randomUUID()}.db"; val db=open(name)
        try {
            val (id,other)=seed(db); val dao=db.weatherForecastDao(); val before=dao.observeForecastWeather(id).first(); val otherBefore=dao.observeForecastWeather(other).first()
            val remote=Remote(); val repo=WeatherForecastRepositoryImpl(remote,db.vineyardDao(),WeatherForecastMapper(),db.weatherCacheDao())
            val good=WeatherResponseFixtures.response()
            for (invalid in listOf(good.copy(hourlyUnits=good.hourlyUnits!! + ("wind_speed_10m" to "mph")),
                good.copy(hourlyWeather=good.hourlyWeather.copy(hourlyTemp2m=List(48) { Double.NaN })),WeatherResponseFixtures.response(23))) {
                remote.response=invalid
                assertIs<NetworkError.SerializationError>(assertIs<Result.Error<*>>(repo.refreshForecast(id,42.7,23.3,fetchedAt)).error)
                assertEquals(before,dao.observeForecastWeather(id).first()); assertEquals(otherBefore,dao.observeForecastWeather(other).first())
            }
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun validResponseReplacesOnlySelectedVineyardAndKeepsNumericDayFlags(): Unit=runBlocking {
        val name="r06b1-success-${UUID.randomUUID()}.db"; val db=open(name)
        try {
            val (id,other)=seed(db); val dao=db.weatherForecastDao(); val otherBefore=dao.observeForecastWeather(other).first()
            val repo=WeatherForecastRepositoryImpl(Remote(),db.vineyardDao(),WeatherForecastMapper(),db.weatherCacheDao())
            assertIs<Result.Success<Unit>>(repo.refreshForecast(id,42.7,23.3,fetchedAt))
            val rows=dao.observeForecastWeather(id).first(); assertEquals(48,rows.size)
            assertTrue(rows.all { it.vineyardId==id && it.forecastCreatedAt==fetchedAt })
            assertFalse(rows[0].isDay); assertTrue(rows[1].isDay)
            assertEquals(otherBefore,dao.observeForecastWeather(other).first())
        } finally { db.close(); context.deleteDatabase(name) }
    }
    @Test fun insertionFailureRollsBackPriorForecastDeletion(): Unit=runBlocking {
        val name="r06b1-rollback-${UUID.randomUUID()}.db"; var db=open(name)
        try {
            val (id,other)=seed(db); val before=db.weatherForecastDao().observeForecastWeather(id).first(); val otherBefore=db.weatherForecastDao().observeForecastWeather(other).first()
            db.close()
            val raw=BundledSQLiteDriver().open(context.getDatabasePath(name).absolutePath)
            try { raw.execSQL("CREATE TRIGGER r06b1_fail_forecast BEFORE INSERT ON weather_forecast BEGIN SELECT RAISE(ABORT, 'Injected write failure'); END") } finally { raw.close() }
            db=open(name)
            val repo=WeatherForecastRepositoryImpl(Remote(),db.vineyardDao(),WeatherForecastMapper(),db.weatherCacheDao())
            assertIs<Result.Error<*>>(repo.refreshForecast(id,42.7,23.3,fetchedAt))
            assertEquals(before,db.weatherForecastDao().observeForecastWeather(id).first())
            assertEquals(otherBefore,db.weatherForecastDao().observeForecastWeather(other).first())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
