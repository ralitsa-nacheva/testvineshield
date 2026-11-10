package com.rncoding.testvineshield.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HarvestRouteSerializationTest {
    @Test fun actualNavigationRegistryRestoresBlockAndHarvestKeys() {
        val json = Json { serializersModule = vineShieldSerializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        for (route in listOf<NavKey>(HarvestListRoute(10, 20), HarvestDetailsRoute(10, 20, 40), HarvestEditorRoute(10, 20), HarvestEditorRoute(10, 20, 40))) {
            assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route)))
        }
    }
}
