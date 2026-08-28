package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherForecastEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface WeatherForecastDao {
    @Upsert
    @Transaction
    suspend fun upsertForecast(forecast: WeatherForecastEntity)

    @Query("Select * From weather_forecast Where vineyard_id= :vineyardId")
    suspend fun observeForecastWeather(vineyardId: Long): Flow<WeatherForecastEntity>

    @Query("Delete From weather_forecast")
    @Transaction
    suspend fun deleteAllForecastWeather()
}