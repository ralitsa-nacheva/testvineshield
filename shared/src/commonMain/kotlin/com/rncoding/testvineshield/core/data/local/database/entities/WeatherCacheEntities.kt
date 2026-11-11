package com.rncoding.testvineshield.core.data.local.database.entities

import androidx.room.*

@Entity(tableName="weather_source", foreignKeys=[ForeignKey(entity=VineyardEntity::class,parentColumns=["vineyard_id"],childColumns=["vineyard_id"],onDelete=ForeignKey.CASCADE)])
data class WeatherSourceEntity(@PrimaryKey @ColumnInfo(name="vineyard_id") val vineyardId: Long, val generation: String)

@Entity(tableName="weather_history_source", foreignKeys=[ForeignKey(entity=WeatherHistoryEntity::class,parentColumns=["weather_id"],childColumns=["weather_id"],onDelete=ForeignKey.CASCADE)])
data class WeatherHistorySourceEntity(
    @PrimaryKey @ColumnInfo(name="weather_id") val weatherId: Long,
    val generation: String, @ColumnInfo(name="fetched_at") val fetchedAt: Long,
    val latitude: Double, val longitude: Double, @ColumnInfo(name="time_zone") val timeZone: String
)

@Entity(tableName="weather_cache_status",primaryKeys=["vineyard_id","kind"],foreignKeys=[ForeignKey(entity=VineyardEntity::class,parentColumns=["vineyard_id"],childColumns=["vineyard_id"],onDelete=ForeignKey.CASCADE)])
data class WeatherCacheStatusEntity(
    @ColumnInfo(name="vineyard_id") val vineyardId: Long, val kind: String, val generation: String,
    val token: String, val state: String, @ColumnInfo(name="attempted_at") val attemptedAt: Long,
    @ColumnInfo(name="succeeded_at") val succeededAt: Long?, @ColumnInfo(name="coverage_start") val coverageStart: Long?,
    @ColumnInfo(name="coverage_end") val coverageEnd: Long?, val hours: Int, val error: String?,
    val latitude: Double, val longitude: Double, @ColumnInfo(name="time_zone") val timeZone: String
)

@Entity(tableName="weather_history_archive",foreignKeys=[ForeignKey(entity=VineyardEntity::class,parentColumns=["vineyard_id"],childColumns=["vineyard_id"],onDelete=ForeignKey.CASCADE)],indices=[Index("vineyard_id")])
data class WeatherHistoryArchiveEntity(
    @PrimaryKey(autoGenerate=true) @ColumnInfo(name="archive_id") val archiveId: Long=0,
    @ColumnInfo(name="vineyard_id") val vineyardId: Long,
    @ColumnInfo(name="weather_id") val weatherId: Long,
    @ColumnInfo(name="archived_at") val archivedAt: Long,
    val generation: String?,val latitude: Double?,val longitude: Double?,
    @ColumnInfo(name="time_zone") val timeZone: String?,
    @ColumnInfo(name="row_json") val rowJson: String,
    // Archive payload version, independent of the Room database version.
    @ColumnInfo(name="row_version",defaultValue="1") val rowVersion: Int=1
)
