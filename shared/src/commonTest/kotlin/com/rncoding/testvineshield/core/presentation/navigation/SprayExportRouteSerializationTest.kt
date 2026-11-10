package com.rncoding.testvineshield.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SprayExportRouteSerializationTest {
    @Test fun actualRegistryRestoresSavedApplicationExportScope() {
        val json = Json { serializersModule = vineShieldSerializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        val route = SprayExportRoute(10, 30)
        assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route)))
    }
}
