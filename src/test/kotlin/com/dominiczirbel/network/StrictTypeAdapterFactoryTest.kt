package com.dominiczirbel.network

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class StrictTypeAdapterFactoryTest {
    private val gson = GsonBuilder()
        .registerTypeAdapterFactory(StrictTypeAdapterFactory)
        .create()
    private val baseGson = Gson()

    private data class TestObject(
        val stringField: String,
        val intField: Int,
        val nullableStringField: String? = null,
        val nestedObject: TestObject? = null
    )

    private data class TestObjectWithGenerics(
        val stringList: List<String>,
        val testObjects: List<TestObject>
    )

    @ParameterizedTest
    @MethodSource("objects")
    fun testSuccess(input: Any?) {
        val output = gson.fromJson(baseGson.toJson(input), input?.let { it::class.java } ?: Any::class.java)
        assertThat(output).isEqualTo(input)
        input?.let {
            if (!input::class.java.isPrimitive && input::class.java != Integer::class.java) {
                println(input::class.java.name)
                assertThat(output).isNotSameInstanceAs(input)
            }
        }
    }

    @Test
    fun testMissingNonNullField() {
        assertThrows<JsonParseException> {
            gson.fromJson(
                """
                {
                    stringField: "abc"
                }
                """.trimIndent(),
                TestObject::class.java
            )
        }
    }

    @Test
    fun testUnusedField() {
        assertThrows<JsonParseException> {
            gson.fromJson(
                """
                {
                    stringField: "abc",
                    intField: 123,
                    unusedField: "xyz"
                }
                """.trimIndent(),
                TestObject::class.java
            )
        }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun objects(): List<Any?> {
            return listOf(
                null,
                1,
                0,
                "",
                listOf<Any>(),
                arrayListOf("a", "b"),
                TestObject(stringField = "abc", intField = 123),
                TestObject(stringField = "abc", intField = 123, nullableStringField = "xyz"),
                TestObject(
                    stringField = "abc",
                    intField = 123,
                    nestedObject = TestObject(stringField = "nested", intField = 42)
                ),
                TestObjectWithGenerics(
                    stringList = listOf("a", "b"),
                    testObjects = listOf(
                        TestObject(stringField = "abc", intField = 123),
                        TestObject(stringField = "abc", intField = 123, nullableStringField = "xyz"),
                        TestObject(
                            stringField = "abc",
                            intField = 123,
                            nestedObject = TestObject(stringField = "nested", intField = 42)
                        ),
                    )
                )
            )
        }
    }
}
