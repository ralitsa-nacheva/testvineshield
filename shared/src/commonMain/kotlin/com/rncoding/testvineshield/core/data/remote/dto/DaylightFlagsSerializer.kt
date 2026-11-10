package com.rncoding.testvineshield.core.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/** Open-Meteo's numeric 0/1 array; never coerce arbitrary numbers or strings. */
object DaylightFlagsSerializer : KSerializer<List<Boolean>> {
    private val integers = ListSerializer(Int.serializer())
    override val descriptor = integers.descriptor
    override fun deserialize(decoder: Decoder): List<Boolean> {
        val json = decoder as? JsonDecoder ?: throw SerializationException("Weather daylight flags require JSON")
        val array = json.decodeJsonElement() as? JsonArray ?: throw SerializationException("is_day must be an array")
        return array.map { element ->
            val value = element as? JsonPrimitive ?: throw SerializationException("is_day must contain numeric flags")
            if (value.isString) throw SerializationException("is_day flags cannot be strings")
            when (value.content) {
                "0" -> false
                "1" -> true
                else -> throw SerializationException("is_day must contain only numeric 0 or 1")
            }
        }
    }
    override fun serialize(encoder: Encoder, value: List<Boolean>) {
        encoder.encodeSerializableValue(integers, value.map { if (it) 1 else 0 })
    }
}
