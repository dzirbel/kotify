package com.dzirbel.kotify.cache

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class CacheObjectTest {
    @Serializable
    data class TestObject(val field: String, val x: Int)

    @ParameterizedTest
    @MethodSource
    fun encodeDecode(value: Any) {
        val obj = CacheObject(id = "id", obj = value)

        val encoded = Json.encodeToString(obj)
        val decoded = Json.decodeFromString<CacheObject>(encoded)

        assertThat(decoded).isEqualTo(obj)
    }

    @Test
    fun classHashClash() {
        val value = TestObject(field = "field", x = 0)
        val obj = CacheObject(id = "id", obj = value, classHash = 123)

        val encoded = Json.encodeToString(obj)

        assertThrows<CacheObject.Serializer.ClassHashChangedException> {
            Json.decodeFromString<CacheObject>(encoded)
        }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun encodeDecode(): List<Any> {
            return listOf(
                "abc",
                TestObject(field = "field", x = 123),
                arrayOf(1, 2, 3)
            )
        }
    }
}
