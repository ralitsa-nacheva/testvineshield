package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface WeatherDao {
    @Upsert
    @Transaction
    suspend fun upsertWeather(weather: WeatherEntity)

    //Observe the weather entries for a vineyard for the specified day/days
    @Query("Select * From weather Where vineyard_id= :vineyardId and timestamp >= :startDay")
    suspend fun observeHistoricalVineyardWeather(vineyardId: Long, startDay: Long): Flow<List<WeatherEntity>>

    @Query("Select * From weather Where vineyard_id = :vineyardId And timestamp = Max(timestamp)")
    suspend fun getLatestWeatherByVineyard(vineyardId: Long): WeatherEntity

    //Observe the last weather entry for a vineyard
    @Query("Select * From weather Where vineyard_id= :vineyardId Order by timestamp DESC Limit 1")
    suspend fun observeLatestVineyardWeather(vineyardId: Long): Flow<WeatherEntity>

    //use this for vineyardwithsummaryaggregate
    @Query("Select temperature_2m from weather Where vineyard_id= :vineyardId and timestamp = Max(timestamp)")
    suspend fun getLatestHourlyTempByVineyard(vineyardId: Long): Double

    @Query("Select weather.timestamp From weather Where vineyard_id= :vineyardId Order by timestamp Limit 1")
    suspend fun getLastWeatherTimestamp(vineyardId: Long): Long

    @Query("Delete From weather")
    @Transaction
    suspend fun deleteAllWeather()
}