package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherHistoryDao {

    @Upsert
    suspend fun upsertWeather(
        weather: WeatherHistoryEntity
    )

    @Query(
        """
        SELECT *
        FROM weather_history
        WHERE vineyard_id = :vineyardId
          AND timestamp >= :startTimestamp
        ORDER BY timestamp ASC
        """
    )
    fun observeHistoricalVineyardWeather(
        vineyardId: Long,
        startTimestamp: Long
    ): Flow<List<WeatherHistoryEntity>>

    @Query(
        """
        SELECT *
        FROM weather_history
        WHERE vineyard_id = :vineyardId
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun getLatestWeatherByVineyard(
        vineyardId: Long
    ): WeatherHistoryEntity?

    @Query(
        """
        SELECT *
        FROM weather_history
        WHERE vineyard_id = :vineyardId
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    fun observeLatestVineyardWeather(
        vineyardId: Long
    ): Flow<WeatherHistoryEntity?>

    @Query(
        """
        SELECT temperature_2m
        FROM weather_history
        WHERE vineyard_id = :vineyardId
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun getLatestHourlyTempByVineyard(
        vineyardId: Long
    ): Double?

    @Query(
        """
        SELECT MAX(timestamp)
        FROM weather_history
        WHERE vineyard_id = :vineyardId
        """
    )
    suspend fun getLastWeatherTimestamp(
        vineyardId: Long
    ): Long?

    @Query(
        """
        DELETE FROM weather_history
        """
    )
    suspend fun deleteAllWeather()
}