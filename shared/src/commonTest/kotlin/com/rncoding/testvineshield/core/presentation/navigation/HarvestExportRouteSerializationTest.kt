package com.rncoding.testvineshield.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HarvestExportRouteSerializationTest {
    @Test fun registryRetainsExplicitSeasonAndAllSeasonScope() {
        val json = Json { serializersModule = vineShieldSerializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        for (route in listOf(HarvestExportRoute(10, 20), HarvestExportRoute(10, 20, "2026/2027"))) {
            assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route)))
        }
    }
}
