package com.dzirbel.kotify.cache

import com.dzirbel.kotify.cache.CacheObject.Companion.hashFields
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class CacheObjectTest {
    @Serializable
    data class TestObject(val field: String, val x: Int)

    data class UnserializableObject(val field: String, val x: Int)

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
        val decoded = Json.decodeFromString<CacheObject>(encoded)

        assertThat(decoded.obj).isInstanceOf(CacheObject.Serializer.ClassHashChangedException::class.java)
    }

    @Test
    fun customJson() {
        val encoded = """
            {
                "id": "id",
                "type": "${TestObject::class.java.typeName}",
                "classHash": ${TestObject::class.hashFields()},
                "obj": {
                    "field": "value",
                    "x": 123
                },
                "cacheTime": 456
            }
        """.trimIndent()
        val expected = CacheObject(id = "id", obj = TestObject(field = "value", x = 123), cacheTime = 456)

        val decoded = Json.decodeFromString<CacheObject>(encoded)

        assertThat(decoded).isEqualTo(expected)
    }

    @Test
    fun deserializeNoSerializer() {
        val encoded = """
            {
                "id": "id",
                "type": "${UnserializableObject::class.java.typeName}",
                "classHash": ${UnserializableObject::class.hashFields()},
                "obj": {
                    "field": "value",
                    "x": 123
                },
                "cacheTime": 456
            }
        """.trimIndent()

        val decoded = Json.decodeFromString<CacheObject>(encoded)

        assertThat(decoded.obj).isInstanceOf(SerializationException::class.java)
        (decoded.obj as Throwable).apply {
            assertThat(message).isEqualTo(
                "Serializer for class 'UnserializableObject' is not found.\n" +
                    "Mark the class as @Serializable or provide the serializer explicitly."
            )
        }
    }

    @Test
    fun deserializeClassNotFound() {
        val encoded = """
            {
                "id": "id",
                "type": "com.unknown.DoesNotExist",
                "classHash": 111,
                "obj": {
                    "field": "value",
                    "x": 123
                },
                "cacheTime": 456
            }
        """.trimIndent()

        val decoded = Json.decodeFromString<CacheObject>(encoded)

        assertThat(decoded.obj).isInstanceOf(ClassNotFoundException::class.java)
        (decoded.obj as Throwable).apply {
            assertThat(message).isEqualTo("com.unknown.DoesNotExist")
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
