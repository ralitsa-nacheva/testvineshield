package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherForecastEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherForecastDao {

    @Insert
    suspend fun insertForecast(
        forecast: List<WeatherForecastEntity>
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
        WHERE vineyard_id = :vineyardId
        """
    )
    suspend fun deleteForecastByVineyard(
        vineyardId: Long
    )

    @Transaction
    suspend fun replaceForecast(
        vineyardId: Long,
        forecast: List<WeatherForecastEntity>
    ) {
        require(forecast.all { it.vineyardId == vineyardId }) {
            "All forecast observations must belong to the specified vineyard."
        }

        require(forecast.map { it.timestamp }.distinct().size == forecast.size) {
            "Forecast observations must have unique timestamps."
        }

        deleteForecastByVineyard(vineyardId)
        insertForecast(forecast)
    }
}