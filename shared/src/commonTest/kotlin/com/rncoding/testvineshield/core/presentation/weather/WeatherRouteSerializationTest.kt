package com.rncoding.testvineshield.core.presentation.weather

import androidx.navigation3.runtime.NavKey
import com.rncoding.testvineshield.core.presentation.navigation.VineyardWeatherRoute
import com.rncoding.testvineshield.core.presentation.navigation.vineShieldSerializersModule
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class WeatherRouteSerializationTest {
    @Test fun actualRegistryRestoresVineyardScope() {
        val json = Json { serializersModule = vineShieldSerializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        val route = VineyardWeatherRoute(42)
        assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route)))
    }
}
