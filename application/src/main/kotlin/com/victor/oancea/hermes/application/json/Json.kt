package com.victor.oancea.hermes.application.json

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper

class Json {
    companion object {
        val OBJECT_MAPPER = objectMapper {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        }

        inline fun <reified T> deserialize(value: String): T {
            return OBJECT_MAPPER.readValue(value, T::class.java)
        }

        private fun deserialize(value: String): Map<String, Any> {
            return deserialize<Map<String, Any>>(value)
        }

        internal fun <T> serializeToString(value: T): String {
            return OBJECT_MAPPER.writeValueAsString(value)
        }

        internal inline fun <T, reified R> serializeToObject(value: T): R {
            return OBJECT_MAPPER.convertValue(value, R::class.java)
        }

        private inline fun objectMapper(initializationBlock: ObjectMapper.() -> Unit): ObjectMapper {
            val objectMapper = ObjectMapper()
            initializationBlock(objectMapper)
            return objectMapper
        }
    }
}

object Extensions {
    fun Any.serializeToString(): String = Json.serializeToString(this)
    fun Any.serializeToMap(): Map<String, Any> = Json.serializeToObject(this)

    fun String.deserializeToMap(): Map<String, Any> = Json.deserialize(this)

    inline fun <reified T> String.deserializeTo(): T = Json.deserialize(this)
}
