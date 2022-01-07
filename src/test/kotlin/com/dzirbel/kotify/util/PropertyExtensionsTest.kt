package com.dzirbel.kotify.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PropertyExtensionsTest {
    class IteratedProperty<V>(values: List<V>) : ReadOnlyProperty<Any?, V> {
        val iterator = values.iterator()
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): V = iterator.next()
    }

    @Test
    fun testMapped() {
        val values = listOf("abc", "de", "f", "ghij", "")

        val mapped: Int by IteratedProperty(values).mapped { it.length }
        values.forEach { value ->
            assertThat(mapped).isEqualTo(value.length)
        }
    }
}
