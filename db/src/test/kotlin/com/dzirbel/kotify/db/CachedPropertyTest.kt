package com.dzirbel.kotify.db

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class CachedPropertyTest {
    @Test
    fun testReadOnlyCachedProperty() {
        var value = 0
        val cachedProperty = ReadOnlyCachedProperty(requireGetInTransaction = false, getter = { value++ })
        assertThat(value).isEqualTo(0)

        assertThat(cachedProperty.cachedOrNull).isNull()
        assertThrows<IllegalStateException> { cachedProperty.cached }

        assertThat(cachedProperty.live).isEqualTo(0)
        assertThat(cachedProperty.cachedOrNull).isEqualTo(0)
        assertThat(cachedProperty.cached).isEqualTo(0)

        assertThat(cachedProperty.live).isEqualTo(1)
        assertThat(cachedProperty.cachedOrNull).isEqualTo(1)
        assertThat(cachedProperty.cached).isEqualTo(1)

        cachedProperty.invalidate()
        assertThat(cachedProperty.cachedOrNull).isNull()
        assertThrows<IllegalStateException> { cachedProperty.cached }

        cachedProperty.loadToCache()
        assertThat(cachedProperty.cachedOrNull).isEqualTo(2)
        assertThat(cachedProperty.cached).isEqualTo(2)
    }

    @Test
    fun testReadOnlyCachedPropertyInTransaction() {
        var value = 0
        val cachedProperty = ReadOnlyCachedProperty(requireGetInTransaction = true, getter = { value++ })

        assertThrows<IllegalStateException> { cachedProperty.live }

        assertThat(cachedProperty.cachedOrNull).isNull()
        assertThrows<IllegalStateException> { cachedProperty.cached }

        val v1 = transaction { cachedProperty.live }
        assertThat(v1).isEqualTo(0)
        assertThat(cachedProperty.cachedOrNull).isEqualTo(0)
        assertThat(cachedProperty.cached).isEqualTo(0)
    }

    @Test
    fun testReadWriteCachedProperty() {
        var value = 0
        val cachedProperty = ReadWriteCachedProperty(
            requireGetInTransaction = false,
            requireSetInTransaction = false,
            getter = { value++ },
            setter = { value = it },
        )

        assertThat(cachedProperty.cachedOrNull).isNull()
        assertThrows<IllegalStateException> { cachedProperty.cached }

        assertThat(cachedProperty.live).isEqualTo(0)
        assertThat(cachedProperty.cachedOrNull).isEqualTo(0)
        assertThat(cachedProperty.cached).isEqualTo(0)

        cachedProperty.set(3)
        assertThat(cachedProperty.cachedOrNull).isEqualTo(3)
        assertThat(cachedProperty.cached).isEqualTo(3)
        assertThat(value).isEqualTo(3)
    }

    @Test
    fun testReadWriteCachedPropertyInTransaction() {
        var value = 0
        val cachedProperty = ReadWriteCachedProperty(
            requireGetInTransaction = true,
            requireSetInTransaction = true,
            getter = { value++ },
            setter = { value = it },
        )

        assertThrows<IllegalStateException> { cachedProperty.live }
        assertThrows<IllegalStateException> { cachedProperty.set(3) }

        transaction { cachedProperty.set(4) }
        assertThat(cachedProperty.cachedOrNull).isEqualTo(4)
        assertThat(cachedProperty.cached).isEqualTo(4)
        assertThat(value).isEqualTo(4)
    }
}
