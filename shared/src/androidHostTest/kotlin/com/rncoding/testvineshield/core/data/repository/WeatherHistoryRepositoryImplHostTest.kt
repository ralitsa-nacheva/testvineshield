
package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import com.rncoding.testvineshield.core.data.local.mappers.WeatherHistoryMapper
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.WeatherHistoryError
import com.rncoding.testvineshield.core.domain.error.DatabaseError
import com.rncoding.testvineshield.core.domain.error.NetworkError


import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WeatherHistoryRepositoryImplHostTest {

    private val vineyardDao = mockk<VineyardDao>()
    private val weatherHistoryDao = mockk<WeatherHistoryDao>()
    private val remoteDataSource = mockk<WeatherRemoteDataSource>()
    private val mapper = mockk<WeatherHistoryMapper>()

    private val repository = WeatherHistoryRepositoryImpl(
        remoteDataSource = remoteDataSource,
        vineyardDao = vineyardDao,
        weatherHistoryDao = weatherHistoryDao,
        weatherHistoryMapper = mapper
    )

    private val vineyardId = 10L
    private val latitude = 42.7
    private val longitude = 23.3
    private val timeZone = "Europe/Sofia"

    private fun stubTimeZone() {
        coEvery {
            vineyardDao.getVineyardTimeZone(vineyardId)
        } returns timeZone
    }

    private fun weatherResponse(): WeatherDto {
        val response = mockk<WeatherDto>()
        every { response.timezone } returns timeZone
        return response
    }

    private fun stubRemote(response: WeatherDto) {
        coEvery {
            remoteDataSource.fetchWeather(
                latitude = latitude,
                longitude = longitude,
                timeZone = timeZone
            )
        } returns response
    }

    private fun stubMapping(
        response: WeatherDto,
        entities: List<WeatherHistoryEntity>
    ) {
        every {
            mapper.weatherDtoToEntity(
                weatherDto = response,
                vineyardId = vineyardId,
                latestCompletedHourStart = any()
            )
        } returns entities
    }

    @Test
    fun invalidVineyardIdIsRejectedBeforeAnyIO(): Unit = runBlocking {
        val result = repository.refreshWeatherHistory(
            vineyardId = 0L,
            latitude = latitude,
            longitude = longitude
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<WeatherHistoryError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            vineyardDao.getVineyardTimeZone(any())
        }
        coVerify(exactly = 0) {
            remoteDataSource.fetchWeather(any(), any(), any())
        }
    }

    @Test
    fun invalidLatitudeIsRejected(): Unit = runBlocking {
        val result = repository.refreshWeatherHistory(
            vineyardId = vineyardId,
            latitude = 91.0,
            longitude = longitude
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<WeatherHistoryError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            remoteDataSource.fetchWeather(any(), any(), any())
        }
    }

    @Test
    fun invalidLongitudeIsRejected(): Unit = runBlocking {
        val result = repository.refreshWeatherHistory(
            vineyardId = vineyardId,
            latitude = latitude,
            longitude = 181.0
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<WeatherHistoryError.InvalidInput>(error.error)

        coVerify(exactly = 0) {
            remoteDataSource.fetchWeather(any(), any(), any())
        }
    }

    @Test
    fun cancellationDuringTimeZoneLookupIsPropagated(): Unit = runBlocking {
        coEvery {
            vineyardDao.getVineyardTimeZone(vineyardId)
        } throws CancellationException("Database read cancelled")

        assertFailsWith<CancellationException> {
            repository.refreshWeatherHistory(
                vineyardId, latitude, longitude
            )
        }

        coVerify(exactly = 0) {
            remoteDataSource.fetchWeather(any(), any(), any())
        }
    }

    @Test
    fun cancellationDuringRemoteFetchIsPropagated(): Unit = runBlocking {
        stubTimeZone()

        coEvery {
            remoteDataSource.fetchWeather(
                latitude, longitude, timeZone
            )
        } throws CancellationException("Network request cancelled")

        assertFailsWith<CancellationException> {
            repository.refreshWeatherHistory(
                vineyardId, latitude, longitude
            )
        }

        verify(exactly = 0) {
            mapper.weatherDtoToEntity(any(), any(), any())
        }
    }

    @Test
    fun timeZoneMismatchReturnsError(): Unit = runBlocking {
        stubTimeZone()

        val response = mockk<WeatherDto>()
        every { response.timezone } returns "UTC"

        stubRemote(response)

        val result = repository.refreshWeatherHistory(
            vineyardId, latitude, longitude
        )

        val error = assertIs<Result.Error<*>>(result)
        val mismatch =
            assertIs<WeatherHistoryError.TimeZoneMismatch>(error.error)

        assertEquals(timeZone, mismatch.expected)
        assertEquals("UTC", mismatch.actual)

        verify(exactly = 0) {
            mapper.weatherDtoToEntity(any(), any(), any())
        }
    }

    @Test
    fun emptyMappedWeatherSkipsDatabaseInsertion(): Unit = runBlocking {
        stubTimeZone()

        val response = weatherResponse()
        stubRemote(response)
        stubMapping(response, emptyList())

        val result = repository.refreshWeatherHistory(
            vineyardId, latitude, longitude
        )

        val success = assertIs<Result.Success<*>>(result)
        val refresh = assertIs<
                com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryRefreshResult
                >(success.data)

        assertEquals(vineyardId, refresh.vineyardId)
        assertEquals(0, refresh.receivedCompletedHours)
        assertEquals(0, refresh.insertedHours)
        assertEquals(0, refresh.ignoredHours)
        assertFalse(refresh.hasNewWeather)

        coVerify(exactly = 0) {
            weatherHistoryDao.insertMissingWeather(any())
        }
    }

    @Test
    fun insertedAndIgnoredHoursAreCountedCorrectly(): Unit = runBlocking {
        stubTimeZone()

        val response = weatherResponse()
        stubRemote(response)

        val first = mockk<WeatherHistoryEntity>()
        val second = mockk<WeatherHistoryEntity>()
        val third = mockk<WeatherHistoryEntity>()

        val entities = listOf(first, second, third)

        stubMapping(response, entities)

        coEvery {
            weatherHistoryDao.insertMissingWeather(entities)
        } returns listOf(11L, -1L, 13L)

        val result = repository.refreshWeatherHistory(
            vineyardId, latitude, longitude
        )

        val success = assertIs<Result.Success<*>>(result)
        val refresh = assertIs<
                com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryRefreshResult
                >(success.data)

        assertEquals(3, refresh.receivedCompletedHours)
        assertEquals(2, refresh.insertedHours)
        assertEquals(1, refresh.ignoredHours)
        assertTrue(refresh.hasNewWeather)

        coVerify(exactly = 1) {
            weatherHistoryDao.insertMissingWeather(entities)
        }
    }

    @Test
    fun cancellationDuringDatabaseInsertionIsPropagated(): Unit = runBlocking {
        stubTimeZone()

        val response = weatherResponse()
        stubRemote(response)

        val entity = mockk<WeatherHistoryEntity>()
        val entities = listOf(entity)

        stubMapping(response, entities)

        coEvery {
            weatherHistoryDao.insertMissingWeather(entities)
        } throws CancellationException("Insert cancelled")

        assertFailsWith<CancellationException> {
            repository.refreshWeatherHistory(
                vineyardId, latitude, longitude
            )
        }
    }


    @Test
    fun missingVineyardReturnsNotFound(): Unit = runBlocking {
        coEvery {
            vineyardDao.getVineyardTimeZone(vineyardId)
        } returns null

        val result = repository.refreshWeatherHistory(
            vineyardId, latitude, longitude
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<DatabaseError.NotFound>(error.error)

        coVerify(exactly = 0) {
            remoteDataSource.fetchWeather(any(), any(), any())
        }
    }

    @Test
    fun timezoneDatabaseFailureReturnsReadError(): Unit = runBlocking {
        coEvery {
            vineyardDao.getVineyardTimeZone(vineyardId)
        } throws IllegalStateException("Database unavailable")

        val result = repository.refreshWeatherHistory(
            vineyardId, latitude, longitude
        )

        val error = assertIs<Result.Error<*>>(result)
        val databaseError =
            assertIs<DatabaseError.Unknown>(error.error)

        assertEquals(
            DatabaseError.Operation.READ,
            databaseError.operation
        )

        coVerify(exactly = 0) {
            remoteDataSource.fetchWeather(any(), any(), any())
        }
    }

    @Test
    fun networkFailureReturnsMappedNetworkError(): Unit = runBlocking {
        stubTimeZone()

        coEvery {
            remoteDataSource.fetchWeather(
                latitude, longitude, timeZone
            )
        } throws IllegalStateException("Unexpected network failure")

        val result = repository.refreshWeatherHistory(
            vineyardId, latitude, longitude
        )

        val error = assertIs<Result.Error<*>>(result)
        assertIs<NetworkError.Unknown>(error.error)

        verify(exactly = 0) {
            mapper.weatherDtoToEntity(any(), any(), any())
        }
    }

    @Test
    fun mappingFailureReturnsSerializationError(): Unit = runBlocking {
        stubTimeZone()

        val response = weatherResponse()
        stubRemote(response)

        every {
            mapper.weatherDtoToEntity(
                weatherDto = response,
                vineyardId = vineyardId,
                latestCompletedHourStart = any()
            )
        } throws IllegalArgumentException("Malformed hourly weather")

        val result = repository.refreshWeatherHistory(
            vineyardId, latitude, longitude
        )

        val error = assertIs<Result.Error<*>>(result)

        assertIs<NetworkError.SerializationError>(error.error)

        coVerify(exactly = 0) {
            weatherHistoryDao.insertMissingWeather(any())
        }
    }

    @Test
    fun insertionFailureReturnsDatabaseInsertError(): Unit = runBlocking {
        stubTimeZone()

        val response = weatherResponse()
        stubRemote(response)

        val entity = mockk<WeatherHistoryEntity>()
        val entities = listOf(entity)

        stubMapping(response, entities)

        coEvery {
            weatherHistoryDao.insertMissingWeather(entities)
        } throws IllegalStateException("Database unavailable")

        val result = repository.refreshWeatherHistory(
            vineyardId, latitude, longitude
        )

        val error = assertIs<Result.Error<*>>(result)
        val databaseError =
            assertIs<DatabaseError.Unknown>(error.error)

        assertEquals(
            DatabaseError.Operation.INSERT,
            databaseError.operation
        )
    }

    @Test
    fun nonFiniteLatitudeIsRejected(): Unit = runBlocking {
        for (invalidLatitude in listOf(
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        )) {
            val result = repository.refreshWeatherHistory(
                vineyardId = vineyardId,
                latitude = invalidLatitude,
                longitude = longitude
            )

            val error = assertIs<Result.Error<*>>(result)
            assertIs<WeatherHistoryError.InvalidInput>(error.error)
        }

        coVerify(exactly = 0) {
            vineyardDao.getVineyardTimeZone(any())
        }
    }

    @Test
    fun nonFiniteLongitudeIsRejected(): Unit = runBlocking {
        for (invalidLongitude in listOf(
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        )) {
            val result = repository.refreshWeatherHistory(
                vineyardId = vineyardId,
                latitude = latitude,
                longitude = invalidLongitude
            )

            val error = assertIs<Result.Error<*>>(result)
            assertIs<WeatherHistoryError.InvalidInput>(error.error)
        }

        coVerify(exactly = 0) {
            vineyardDao.getVineyardTimeZone(any())
        }
    }



    @Test
    fun cancellationAfterTimezoneLookupPreventsRemoteFetch(): Unit = runBlocking {
        val childJob = CompletableDeferred<Job>()

        coEvery {
            vineyardDao.getVineyardTimeZone(vineyardId)
        } coAnswers {
            childJob.await().cancel(
                CancellationException("Cancelled after timezone lookup")
            )
            timeZone
        }

        val deferred = async {
            childJob.complete(
                currentCoroutineContext().job
            )

            repository.refreshWeatherHistory(
                vineyardId = vineyardId,
                latitude = latitude,
                longitude = longitude
            )
        }

        assertFailsWith<CancellationException> {
            deferred.await()
        }

        coVerify(exactly = 1) {
            vineyardDao.getVineyardTimeZone(vineyardId)
        }

        coVerify(exactly = 0) {
            remoteDataSource.fetchWeather(any(), any(), any())
        }
    }

}
