package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

internal class CaseInsensitiveEnumSerializerTest {
    @Serializable(with = Enum.Serializer::class)
    private enum class Enum {
        FIRST_VALUE,
        VALUE_2,
        V3;

        object Serializer : CaseInsensitiveEnumSerializer<Enum>(Enum::class)
    }

    @Serializable
    private data class SimpleWrapper(
        val v1: Enum = Enum.FIRST_VALUE,
        val v2: Enum = Enum.VALUE_2,
        val n1: Int = 123,
    ) {
        companion object {
            const val defaultJsonString = """{"v1":"FIRST_VALUE","v2":"VALUE_2","n1":123}"""
            const val defaultJsonStringLower = """{"v1":"first_value","v2":"value_2","n1":123}"""
        }
    }

    @Test
    fun testSerialize() {
        assertThat(json.encodeToString(Enum.FIRST_VALUE)).isEqualTo("\"FIRST_VALUE\"")
        assertThat(json.encodeToString(Enum.VALUE_2)).isEqualTo("\"VALUE_2\"")
        assertThat(json.encodeToString(Enum.V3)).isEqualTo("\"V3\"")

        assertThat(json.encodeToString(SimpleWrapper())).isEqualTo(SimpleWrapper.defaultJsonString)
    }

    @Test
    fun testDeserialize() {
        assertThat(json.decodeFromString<Enum>("\"FIRST_VALUE\"")).isEqualTo(Enum.FIRST_VALUE)
        assertThat(json.decodeFromString<Enum>("\"first_value\"")).isEqualTo(Enum.FIRST_VALUE)
        assertThat(json.decodeFromString<Enum>("\"FiRsT_vAlUe\"")).isEqualTo(Enum.FIRST_VALUE)

        assertThat(json.decodeFromString<SimpleWrapper>(SimpleWrapper.defaultJsonString)).isEqualTo(SimpleWrapper())
        assertThat(json.decodeFromString<SimpleWrapper>(SimpleWrapper.defaultJsonStringLower))
            .isEqualTo(SimpleWrapper())
    }

    companion object {
        private val json = Json {
            encodeDefaults = true
        }
    }
}
