package com.dominiczirbel.network

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

// TODO test write() delegation
// TODO test all parameter combinations
// TODO test SerializedName
// TODO test OptionalField
// TODO add more complicated test cases (Maps, large classes, etc)
internal class StrictReflectiveTypeAdapterFactoryTest {
    data class Parameters(
        val requireAllClassFieldsUsed: Boolean = true,
        val requireAllJsonFieldsUsed: Boolean = true,
        val allowUnusedNulls: Boolean = true
    ) {
        fun toGson(): Gson {
            return GsonBuilder()
                .registerTypeAdapterFactory(
                    StrictReflectiveTypeAdapterFactory(
                        requireAllClassFieldsUsed = requireAllClassFieldsUsed,
                        requireAllJsonFieldsUsed = requireAllJsonFieldsUsed,
                        allowUnusedNulls = allowUnusedNulls,
                    )
                )
                .create()
        }
    }

    data class ExceptionCase(
        val input: Map<String, *>,
        val expectedClass: Class<*>,
        val exceptionMessage: String? = null,
        val parameters: Parameters = Parameters()
    )

    data class SuccessCase(
        val input: Any?,
        val expectedValue: Any? = input,
        val expectedClass: Class<*> = expectedValue?.let { it::class.java } ?: Any::class.java,
        val sameInstance: Boolean = false,
        val parameters: Parameters = Parameters()
    )

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

    private val baseGson = Gson()

    @ParameterizedTest
    @MethodSource("successCases")
    fun testSuccess(successCase: SuccessCase) {
        val gson = successCase.parameters.toGson()
        val output = gson.fromJson(baseGson.toJson(successCase.input), successCase.expectedClass)
        assertThat(output).isEqualTo(successCase.expectedValue)
        if (successCase.sameInstance) {
            assertThat(output).isSameInstanceAs(successCase.input)
        } else {
            assertThat(output).isNotSameInstanceAs(successCase.input)
        }
    }

    @ParameterizedTest
    @MethodSource("exceptionCases")
    fun testExceptions(exceptionCase: ExceptionCase) {
        val gson = exceptionCase.parameters.toGson()
        val exception = assertThrows<JsonParseException> {
            gson.fromJson(
                baseGson.toJson(exceptionCase.input),
                exceptionCase.expectedClass
            )
        }
        exceptionCase.exceptionMessage?.let { assertThat(exception.message).isEqualTo(it) }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun exceptionCases(): List<ExceptionCase> {
            return listOf(
                ExceptionCase(
                    input = mapOf("stringField" to "abc"),
                    expectedClass = TestObject::class.java,
                    exceptionMessage = "Model class com.dominiczirbel.network.StrictReflectiveTypeAdapterFactoryTest" +
                        "\$TestObject required field(s) which were not present in the JSON: [intField]"
                ),
                ExceptionCase(
                    input = mapOf(
                        "stringField" to "abc",
                        "intField" to 123,
                        "unusedField" to "xyz"
                    ),
                    expectedClass = TestObject::class.java,
                    exceptionMessage = "Model class com.dominiczirbel.network.StrictReflectiveTypeAdapterFactoryTest" +
                        "\$TestObject does not contain a field for JSON property `unusedField` with value `xyz`"
                ),
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun successCases(): List<SuccessCase> {
            return listOf(
                // test primitives are delegated correctly
                SuccessCase(input = null, expectedValue = null, sameInstance = true),
                SuccessCase(input = 1, sameInstance = true),
                SuccessCase(input = 0, sameInstance = true),
                SuccessCase(input = ""),
                SuccessCase(input = listOf<Any>()),
                SuccessCase(input = arrayListOf("a", "b")),

                // test reflection
                SuccessCase(input = TestObject(stringField = "abc", intField = 123)),
                SuccessCase(input = TestObject(stringField = "abc", intField = 123, nullableStringField = "xyz")),
                SuccessCase(
                    input = TestObject(
                        stringField = "abc",
                        intField = 123,
                        nestedObject = TestObject(stringField = "nested", intField = 42)
                    )
                ),
                SuccessCase(
                    input = TestObjectWithGenerics(
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
                ),
            )
        }
    }
}
