package com.dzirbel.kotify.network.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CaseInsensitiveEnumSerializerTest {
    @Serializable(with = Enum.Serializer::class)
    private enum class Enum {
        FIRST_VALUE,
        VALUE_2,
        V3,
        ;

        object Serializer : CaseInsensitiveEnumSerializer<Enum>(Enum::class)
    }

    @Serializable(with = EnumWithFallback.Serializer::class)
    private enum class EnumWithFallback {
        A_VALUE,
        FALLBACK_VALUE,
        ;

        object Serializer : CaseInsensitiveEnumSerializer<EnumWithFallback>(
            enumClass = EnumWithFallback::class,
            fallbackValue = FALLBACK_VALUE,
        )
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
            const val invalidString = """{"v1":"invalid","v2":"VALUE_2","n1":123}"""
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

    @Test
    fun testDeserializeInvalid() {
        assertThrows<IllegalArgumentException> { json.decodeFromString<Enum>("\"invalid\"") }
        assertThrows<IllegalArgumentException> { json.decodeFromString<SimpleWrapper>(SimpleWrapper.invalidString) }

        assertThat(json.decodeFromString<EnumWithFallback>("\"A_VALUE\"")).isEqualTo(EnumWithFallback.A_VALUE)
        assertThat(json.decodeFromString<EnumWithFallback>("\"\"")).isEqualTo(EnumWithFallback.FALLBACK_VALUE)
        assertThat(json.decodeFromString<EnumWithFallback>("\"invalid\"")).isEqualTo(EnumWithFallback.FALLBACK_VALUE)
    }

    companion object {
        private val json = Json {
            encodeDefaults = true
        }
    }
}
