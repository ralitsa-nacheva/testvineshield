package com.rncoding.testvineshield.core.data.remote

import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class WeatherWireContractTest {
    @Test fun numericDayFlagsDecodeAndEncodeWithExactWireType() {
        val dto = WeatherResponseFixtures.response()
        assertEquals(listOf(false,true,false,true), dto.hourlyWeather.isDay.take(4))
        WeatherResponseValidator.validate(dto)
        val wire = Json.parseToJsonElement(Json.encodeToString(dto)).jsonObject
        val flags = wire.getValue("hourly").jsonObject.getValue("is_day").jsonArray
        assertEquals(listOf(0,1,0,1), flags.take(4).map { it.jsonPrimitive.int })
        assertTrue(flags.all { !it.jsonPrimitive.isString })
    }
    @Test fun invalidDayFlagNumbersStringsBooleansAndNullsAreRejected() {
        for (flag in listOf<JsonElement>(JsonPrimitive(2),JsonPrimitive(-1),JsonPrimitive("1"),JsonPrimitive(true),JsonNull)) {
            val text = WeatherResponseFixtures.replaceHourly("is_day", JsonArray(List(48) { flag }))
            assertFailsWith<SerializationException> { Json.decodeFromString<WeatherDto>(text) }
        }
    }
    @Test fun missingRequiredArrayAndNullMeasurementCannotBecomeZero() {
        val root = Json.parseToJsonElement(WeatherResponseFixtures.wire()).jsonObject
        val missing = JsonObject(root + ("hourly" to JsonObject(root.getValue("hourly").jsonObject - "soil_temperature_0cm")))
        assertFailsWith<SerializationException> { Json.decodeFromString<WeatherDto>(missing.toString()) }
        assertFailsWith<SerializationException> {
            Json.decodeFromString<WeatherDto>(WeatherResponseFixtures.replaceHourly("soil_temperature_0cm", JsonNull))
        }
        assertFailsWith<SerializationException> {
            Json.decodeFromString<WeatherDto>(WeatherResponseFixtures.replaceHourly("temperature_2m", JsonArray(List(48) { JsonNull })))
        }
    }
    @Test fun omittedUnitMetadataIsNotSilentlyAssumed() {
        val root = Json.parseToJsonElement(WeatherResponseFixtures.wire()).jsonObject
        val decoded = Json.decodeFromString<WeatherDto>(JsonObject(root - "hourly_units").toString())
        assertNull(decoded.hourlyUnits)
        assertFailsWith<IllegalArgumentException> { WeatherResponseValidator.validate(decoded) }
    }
}
