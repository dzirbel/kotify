package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class TakingIfTest {
    @Test
    fun `take true`() {
        var called = false
        val result = takingIf(true) {
            called = true
            "true"
        }

        assertThat(result).isEqualTo("true")
        assertThat(called).isTrue()
    }

    @Test
    fun `take false`() {
        var called = false
        val result = takingIf(false) {
            called = true
            "false"
        }

        assertThat(result).isEqualTo(null)
        assertThat(called).isFalse()
    }
}
