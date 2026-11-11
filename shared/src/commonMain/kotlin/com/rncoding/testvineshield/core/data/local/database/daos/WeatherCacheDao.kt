package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.*
import com.rncoding.testvineshield.core.data.local.database.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Request identity captured in a short DB transaction; no transaction spans HTTP. */
data class WeatherCacheRequest(val vineyardId: Long,val ownerId: Long,val generation: String,val token: String,
    val kind: String,val latitude: Double,val longitude: Double,val timeZone: String,val attemptedAt: Long)

data class WeatherCacheCoverage(val first: Long?,val last: Long?,val count: Int)

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM vineyard WHERE vineyard_id=:id LIMIT 1")
    suspend fun vineyard(id: Long): VineyardEntity?
    @Query("SELECT generation FROM weather_source WHERE vineyard_id=:id")
    suspend fun generation(id: Long): String?
    @Query("SELECT lower(hex(randomblob(16)))")
    suspend fun newToken(): String
    @Query("SELECT * FROM weather_cache_status WHERE vineyard_id=:id AND kind=:kind LIMIT 1")
    suspend fun status(id: Long,kind: String): WeatherCacheStatusEntity?
    @Upsert suspend fun writeStatus(row: WeatherCacheStatusEntity)
    @Query("SELECT s.* FROM weather_cache_status s JOIN weather_source g ON g.vineyard_id=s.vineyard_id AND g.generation=s.generation WHERE s.vineyard_id=:id ORDER BY s.kind")
    fun observeStatus(id: Long): Flow<List<WeatherCacheStatusEntity>>
    @Query("SELECT w.* FROM weather_history w JOIN weather_history_source p ON p.weather_id=w.weather_id JOIN weather_source g ON g.vineyard_id=w.vineyard_id AND g.generation=p.generation WHERE w.vineyard_id=:id AND w.timestamp>=:start ORDER BY w.timestamp")
    fun observeHistory(id: Long,start: Long): Flow<List<WeatherHistoryEntity>>
    @Query("SELECT w.* FROM weather_history w JOIN weather_history_source p ON p.weather_id=w.weather_id JOIN weather_source g ON g.vineyard_id=w.vineyard_id AND g.generation=p.generation WHERE w.vineyard_id=:id ORDER BY w.timestamp DESC LIMIT 1")
    fun observeLatestHistory(id: Long): Flow<WeatherHistoryEntity?>
    @Query("SELECT w.* FROM weather_history w JOIN weather_history_source p ON p.weather_id=w.weather_id JOIN weather_source g ON g.vineyard_id=w.vineyard_id AND g.generation=p.generation WHERE w.vineyard_id=:id AND w.timestamp>=:start AND w.timestamp<:end ORDER BY w.timestamp")
    suspend fun riskWindow(id: Long,start: Long,end: Long): List<WeatherHistoryEntity>
    @Query("SELECT w.* FROM weather_forecast w JOIN weather_cache_status s ON s.vineyard_id=w.vineyard_id AND s.kind='forecast' JOIN weather_source g ON g.vineyard_id=s.vineyard_id AND g.generation=s.generation WHERE w.vineyard_id=:id AND s.succeeded_at IS NOT NULL ORDER BY w.timestamp")
    fun observeForecast(id: Long): Flow<List<WeatherForecastEntity>>
    @Query("SELECT * FROM weather_history WHERE vineyard_id=:id AND timestamp=:time LIMIT 1")
    suspend fun historyAt(id: Long,time: Long): WeatherHistoryEntity?
    @Query("SELECT * FROM weather_history_source WHERE weather_id=:id LIMIT 1")
    suspend fun historySource(id: Long): WeatherHistorySourceEntity?
    @Insert suspend fun archive(row: WeatherHistoryArchiveEntity): Long
    @Query("SELECT * FROM weather_history_archive WHERE vineyard_id=:id ORDER BY archive_id")
    suspend fun archives(id: Long): List<WeatherHistoryArchiveEntity>
    @Insert suspend fun insertHistory(row: WeatherHistoryEntity): Long
    @Update suspend fun updateHistory(row: WeatherHistoryEntity)
    @Upsert suspend fun writeHistorySource(row: WeatherHistorySourceEntity)
    @Query("DELETE FROM weather_forecast WHERE vineyard_id=:id") suspend fun clearForecast(id: Long)
    @Insert suspend fun insertForecast(rows: List<WeatherForecastEntity>)
    @Query("DELETE FROM disease_risk_model_state WHERE vineyard_id=:id") suspend fun clearModelState(id: Long)
    @Query("DELETE FROM disease_risk_evaluation WHERE vineyard_id=:id") suspend fun clearEvaluations(id: Long)
    @Query("DELETE FROM weather_calculation WHERE vineyard_id=:id") suspend fun clearCalculations(id: Long)
    @Query("UPDATE disease_alert SET alert_status='invalidated' WHERE vineyard_id=:id AND alert_status='active'") suspend fun invalidateAlerts(id: Long)

    @Transaction
    suspend fun begin(id: Long,latitude: Double,longitude: Double,zone: String,kind: String,now: Long): WeatherCacheRequest? {
        require(kind=="history" || kind=="forecast"); require(now>0)
        val parent=vineyard(id) ?: return null
        if(parent.latitude!=latitude || parent.longitude!=longitude || parent.timeZone!=zone) return null
        val source=generation(id) ?: error("Weather source tracking is missing")
        val token=newToken()
        val old=status(id,kind)?.takeIf { it.generation==source }
        writeStatus(WeatherCacheStatusEntity(id,kind,source,token,"started",now,old?.succeededAt,old?.coverageStart,
            old?.coverageEnd,old?.hours ?: 0,null,latitude,longitude,zone))
        return WeatherCacheRequest(id,parent.userId,source,token,kind,latitude,longitude,zone,now)
    }
    suspend fun isCurrent(request: WeatherCacheRequest): Boolean {
        val parent=vineyard(request.vineyardId) ?: return false
        return parent.userId==request.ownerId && parent.latitude==request.latitude && parent.longitude==request.longitude &&
            parent.timeZone==request.timeZone && generation(request.vineyardId)==request.generation &&
            status(request.vineyardId,request.kind)?.let { it.token==request.token && it.state=="started" }==true
    }
    @Query("SELECT MIN(w.timestamp) AS 'first',MAX(w.timestamp) AS 'last',COUNT(*) AS 'count' FROM weather_history w JOIN weather_history_source p ON p.weather_id=w.weather_id JOIN weather_source g ON g.vineyard_id=w.vineyard_id AND g.generation=p.generation WHERE w.vineyard_id=:id")
    suspend fun coverage(id: Long): WeatherCacheCoverage
    @Transaction
    suspend fun completeHistory(request: WeatherCacheRequest,rows: List<WeatherHistoryEntity>,now: Long): Int? {
        require(request.kind=="history"); require(rows.all { it.vineyardId==request.vineyardId })
        require(rows.map { it.timestamp }.distinct().size==rows.size)
        if(!isCurrent(request)) return null
        if(status(request.vineyardId,"history")?.succeededAt==null) {
            clearModelState(request.vineyardId); clearEvaluations(request.vineyardId); clearCalculations(request.vineyardId); invalidateAlerts(request.vineyardId)
        }
        var inserted=0
        for(row in rows) {
            val old=historyAt(request.vineyardId,row.timestamp)
            val origin=old?.let { historySource(it.weatherId) }
            if(old!=null && origin?.generation==request.generation) continue
            if(old!=null) archive(WeatherHistoryArchiveEntity(vineyardId=request.vineyardId,weatherId=old.weatherId,
                archivedAt=now,generation=origin?.generation,latitude=origin?.latitude,longitude=origin?.longitude,
                timeZone=origin?.timeZone,rowJson=weatherArchiveJson.encodeToString(old)))
            val id=if(old==null) { inserted++; insertHistory(row.copy(weatherId=0)) }
                else { updateHistory(row.copy(weatherId=old.weatherId)); old.weatherId }
            writeHistorySource(WeatherHistorySourceEntity(id,request.generation,now,request.latitude,request.longitude,request.timeZone))
        }
        val all=coverage(request.vineyardId)
        val previous=checkNotNull(status(request.vineyardId,"history"))
        writeStatus(previous.copy(state="succeeded",succeededAt=now,coverageStart=all.first,
            coverageEnd=all.last,hours=all.count,error=null))
        return inserted
    }
    @Transaction
    suspend fun completeForecast(request: WeatherCacheRequest,rows: List<WeatherForecastEntity>,now: Long): Boolean {
        require(request.kind=="forecast"); require(rows.isNotEmpty() && rows.all { it.vineyardId==request.vineyardId })
        require(rows.map { it.timestamp }.distinct().size==rows.size)
        if(!isCurrent(request)) return false
        clearForecast(request.vineyardId); insertForecast(rows)
        val previous=checkNotNull(status(request.vineyardId,"forecast"))
        writeStatus(previous.copy(state="succeeded",succeededAt=now,coverageStart=rows.first().timestamp,
            coverageEnd=rows.last().timestamp,hours=rows.size,error=null))
        return true
    }
    @Transaction
    suspend fun fail(request: WeatherCacheRequest,code: String) {
        if(!isCurrent(request)) return
        val previous=checkNotNull(status(request.vineyardId,request.kind))
        writeStatus(previous.copy(state="failed",error=code.take(40)))
    }
}

// Internal archive preserves even unusual legacy floating values; it is not a public JSON export format.
private val weatherArchiveJson=Json { encodeDefaults=true; allowSpecialFloatingPointValues=true }
