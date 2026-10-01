package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "weather_forecast", foreignKeys = [ForeignKey(
    entity = VineyardEntity::class,
    parentColumns = ["vineyard_id"],
    childColumns = ["vineyard_id"],
    onDelete = ForeignKey.CASCADE
)], indices = [
    Index(value = ["vineyard_id", "timestamp"], unique = true),
    Index(value = ["timestamp"])
])
data class WeatherForecastEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "forecast_id")
    val weatherId: Long,
    @ColumnInfo(name = "vineyard_id")
    val vineyardId: Long,
    val timestamp: Long,
    @ColumnInfo(name = "temperature_2m")
    val hourlyTemp2m: Double,
    @ColumnInfo(name = "relative_humidity_2m")
    val relativeHumidity2m: Double,
    @ColumnInfo(name = "dew_point_2m")
    val dewPoint2m: Double,
    @ColumnInfo(name = "cloud_cover")
    val cloudCover: Double,
    @ColumnInfo(name = "wind_speed_10m")
    val windSpeed10m: Double,
    val precipitation: Double,
    val rain: Double,
    val showers: Double,
    val snowfall: Double,
    @ColumnInfo(name = "snow_depth")
    val snowDepth: Double,
    @ColumnInfo(name = "weather_code")
    val weatherCode: Int,
    @ColumnInfo(name = "freezing_level_height")
    val freezingLevelHeight: Double,
    val visibility: Double,
    @ColumnInfo(name = "soil_temperature_0cm")
    val soilTemperature0cm: Double,
    @ColumnInfo(name = "soil_temperature_6cm")
    val soilTemperature6cm: Double,
    @ColumnInfo(name = "soil_moisture_0_to_1cm")
    val soilMoisture0to1cm: Double,
    @ColumnInfo(name = "soil_moisture_1_to_3cm")
    val soilMoisture1to3cm: Double,
    @ColumnInfo(name = "is_day")
    val isDay: Boolean,
    @ColumnInfo(name = "forecast_created_at")
    val forecastCreatedAt: Long
)
