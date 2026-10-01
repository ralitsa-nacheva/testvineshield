package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherForecastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherForecastDao {

    @Upsert
    suspend fun upsertForecast(
        forecast: WeatherForecastEntity
    )

    @Query(
        """
        SELECT *
        FROM weather_forecast
        WHERE vineyard_id = :vineyardId
        ORDER BY timestamp ASC
        """
    )
    fun observeForecastWeather(
        vineyardId: Long
    ): Flow<List<WeatherForecastEntity>>

    @Query(
        """
        SELECT *
        FROM weather_forecast
        WHERE vineyard_id = :vineyardId
          AND timestamp >= :fromTimestamp
        ORDER BY timestamp ASC
        LIMIT 1
        """
    )
    fun observeNextForecastWeather(
        vineyardId: Long,
        fromTimestamp: Long
    ): Flow<WeatherForecastEntity?>

    @Query(
        """
        DELETE FROM weather_forecast
        """
    )
    suspend fun deleteAllForecastWeather()
}