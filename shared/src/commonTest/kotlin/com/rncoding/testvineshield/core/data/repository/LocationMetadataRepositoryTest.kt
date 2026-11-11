package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.domain.datamodels.VineyardMapPoint
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.error.Result
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class LocationMetadataRepositoryTest {
    private val point=checkNotNull(VineyardMapPoint.from(42.7,23.3))
    private fun client(engine: MockEngine)=HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys=true }) } }
    @Test fun explicitLookupSendsOnlyPointAndWeatherMetadataOptions() = runTest {
        var calls=0
        val engine=MockEngine { request ->
            calls++; assertEquals("api.open-meteo.com",request.url.host); assertEquals("/v1/forecast",request.url.encodedPath)
            assertEquals("42.7",request.url.parameters["latitude"]); assertEquals("23.3",request.url.parameters["longitude"])
            assertEquals("auto",request.url.parameters["timezone"])
            assertEquals(setOf("latitude","longitude","timezone","current","forecast_days"),request.url.parameters.names())
            respond("""{"timezone":"Europe/Sofia","unused":123}""",headers=headersOf(HttpHeaders.ContentType,"application/json"))
        }
        val client=client(engine)
        try { val repo=LocationMetadataRepositoryImpl(client); assertEquals(0,calls)
            assertEquals("Europe/Sofia",assertIs<Result.Success<String>>(repo.suggestTimeZone(point)).data); assertEquals(1,calls)
        } finally { client.close(); client.engine.close() }
    }
    @Test fun unsupportedZoneAndHttpErrorAreTyped() = runTest {
        for((body,status) in listOf("""{"timezone":"Invalid/Zone"}""" to HttpStatusCode.OK,"""{"reason":"limited"}""" to HttpStatusCode.TooManyRequests)) {
            val client=client(MockEngine { respond(body,status,headersOf(HttpHeaders.ContentType,"application/json")) })
            try { val error=assertIs<Result.Error<*>>(LocationMetadataRepositoryImpl(client).suggestTimeZone(point)).error
                if(status==HttpStatusCode.OK) assertIs<NetworkError.SerializationError>(error)
                else assertEquals(429,assertIs<NetworkError.HttpError>(error).code)
            } finally { client.close(); client.engine.close() }
        }
    }
    @Test fun cancellationPropagatesThroughMetadataAdapter() = runTest {
        val entered=CompletableDeferred<Unit>(); val cleaned=CompletableDeferred<Unit>()
        val client=client(MockEngine { try { entered.complete(Unit); awaitCancellation() } finally { cleaned.complete(Unit) } })
        try { val job=launch { LocationMetadataRepositoryImpl(client).suggestTimeZone(point) }
            entered.await(); job.cancelAndJoin(); cleaned.await(); assertTrue(job.isCancelled)
        } finally { client.close(); client.engine.close() }
    }
}
