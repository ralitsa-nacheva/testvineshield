package com.rncoding.testvineshield.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.*

class ActivityRouteSerializationTest {
    @Test fun actualRegistryRestoresWholeVineyardBlockDetailsAndCompletionKeys() {
        val json=Json {serializersModule=vineShieldSerializersModule}
        val serializer=PolymorphicSerializer(NavKey::class)
        listOf<NavKey>(ActivitiesRoute,ActivityListRoute(10),ActivityListRoute(10,20),ActivityDetailsRoute(10,30),
            ActivityEditorRoute(10,20),ActivityEditorRoute(10,activityId=30,complete=true)).forEach {route ->
            assertEquals(route,json.decodeFromString(serializer,json.encodeToString(serializer,route)))
        }
    }
}
