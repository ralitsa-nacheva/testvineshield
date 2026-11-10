package com.rncoding.testvineshield.core.data.remote

import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

/** Independent explicit wire fixture, not generated through the production DTO serializer. */
internal object WeatherResponseFixtures {
    val units = Json.parseToJsonElement("""{
        "time":"unixtime","temperature_2m":"°C","relative_humidity_2m":"%","dew_point_2m":"°C",
        "cloud_cover":"%","wind_speed_10m":"km/h","precipitation":"mm","rain":"mm","showers":"mm",
        "snowfall":"cm","snow_depth":"m","weather_code":"wmo code","freezing_level_height":"m",
        "visibility":"m","soil_temperature_0cm":"°C","soil_temperature_6cm":"°C",
        "soil_moisture_0_to_1cm":"m³/m³","soil_moisture_1_to_3cm":"m³/m³","is_day":""
    }""").jsonObject
    fun wire(hours: Int = 48, startSeconds: Long = 1_800_000_000L): String = buildJsonObject {
        put("latitude",42.7); put("longitude",23.3); put("elevation",550.0); put("timezone","Europe/Sofia")
        put("hourly_units", units)
        putJsonObject("hourly") {
            put("time", JsonArray(List(hours) { JsonPrimitive(startSeconds + it * 3600L) }))
            val values = mapOf("temperature_2m" to 18.0, "relative_humidity_2m" to 65.0, "dew_point_2m" to 10.0,
                "cloud_cover" to 30.0, "wind_speed_10m" to 7.0, "precipitation" to 0.0, "rain" to 0.0,
                "showers" to 0.0, "snowfall" to 0.0, "snow_depth" to 0.0, "freezing_level_height" to 2000.0,
                "visibility" to 10000.0, "soil_temperature_0cm" to 14.0, "soil_temperature_6cm" to 12.0,
                "soil_moisture_0_to_1cm" to 0.3, "soil_moisture_1_to_3cm" to 0.4)
            values.forEach { (key,value) -> put(key,JsonArray(List(hours) { JsonPrimitive(value) })) }
            put("weather_code",JsonArray(List(hours) { JsonPrimitive(61) }))
            put("is_day",JsonArray(List(hours) { JsonPrimitive(it % 2) }))
        }
    }.toString()
    fun response(hours: Int = 48, startSeconds: Long = 1_800_000_000L): WeatherDto = Json.decodeFromString(wire(hours,startSeconds))
    fun replaceHourly(key: String, value: JsonElement): String {
        val root = Json.parseToJsonElement(wire()).jsonObject
        return JsonObject(root + ("hourly" to JsonObject(root.getValue("hourly").jsonObject + (key to value)))).toString()
    }
}
