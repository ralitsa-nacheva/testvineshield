package com.rncoding.testvineshield.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.*

class BlockRouteSerializationTest {
    @Test fun actualNavigationRegistryRestoresAllBlockKeys() {
        val json = Json {serializersModule = vineShieldSerializersModule}
        val serializer = PolymorphicSerializer(NavKey::class)
        listOf<NavKey>(BlocksRoute(10), BlockDetailsRoute(10, 20), BlockEditorRoute(10), BlockEditorRoute(10, 20)).forEach {
            assertEquals(it, json.decodeFromString(serializer, json.encodeToString(serializer, it)))
        }
    }
}
