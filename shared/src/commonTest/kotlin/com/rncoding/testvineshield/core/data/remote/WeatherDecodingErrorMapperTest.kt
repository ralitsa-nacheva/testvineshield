package com.rncoding.testvineshield.core.data.remote
import com.rncoding.testvineshield.core.domain.error.NetworkError
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlin.test.*
class WeatherDecodingErrorMapperTest {
    @Test fun decodingErrorsStayTypedIncludingOneWrapper() {
        assertIs<NetworkError.SerializationError>(WeatherNetworkErrorMapper.map(SerializationException("bad JSON")))
        assertIs<NetworkError.SerializationError>(WeatherNetworkErrorMapper.map(IllegalStateException("conversion",SerializationException("bad JSON"))))
        assertIs<NetworkError.Unknown>(WeatherNetworkErrorMapper.map(IllegalStateException("other")))
    }
    @Test fun cancellationIsNeverAnErrorResult() {
        assertFailsWith<CancellationException> { WeatherNetworkErrorMapper.map(CancellationException("cancel")) }
    }
}
