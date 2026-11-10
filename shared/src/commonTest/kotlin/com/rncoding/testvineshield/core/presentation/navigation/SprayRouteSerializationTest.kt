package com.rncoding.testvineshield.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SprayRouteSerializationTest {
    @Test fun actualRegistryRestoresSprayActivityScope() {
        val json = Json { serializersModule = vineShieldSerializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        val route = SprayRoute(10, 30)
        assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route)))
    }
}
