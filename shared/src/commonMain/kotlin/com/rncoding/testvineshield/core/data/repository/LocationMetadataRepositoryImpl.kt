package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.domain.datamodels.VineyardMapPoint
import com.rncoding.testvineshield.core.domain.repository.LocationMetadataRepository
import com.rncoding.testvineshield.core.domain.time.VineyardTimeZoneCatalog
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.data.remote.WeatherNetworkErrorMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.plugins.expectSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable private data class LocationTimeZoneResponse(val timezone: String)

/** Explicit user request only; no reverse-geocoder, account/name, GPS or telemetry payload. */
class LocationMetadataRepositoryImpl(private val client: HttpClient,private val zones: VineyardTimeZoneCatalog=VineyardTimeZoneCatalog()) : LocationMetadataRepository {
    override suspend fun suggestTimeZone(point: VineyardMapPoint): Result<String,AppError> = try {
        val response=client.get("https://api.open-meteo.com/v1/forecast") {
            expectSuccess=true
            parameter("latitude",point.latitude); parameter("longitude",point.longitude)
            parameter("timezone","auto"); parameter("current","temperature_2m"); parameter("forecast_days",1)
        }.body<LocationTimeZoneResponse>()
        if(zones.contains(response.timezone)) Result.Success(response.timezone)
        else Result.Error(NetworkError.SerializationError())
    } catch(e: CancellationException) { throw e }
    catch(e: Exception) { Result.Error(WeatherNetworkErrorMapper.map(e)) }
}
