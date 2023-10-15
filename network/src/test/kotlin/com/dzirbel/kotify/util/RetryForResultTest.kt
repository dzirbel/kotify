package com.dzirbel.kotify.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RetryForResultTest {
    @Test
    fun success() {
        val result = retryForResult(attempts = 3) { "success" }

        assertThat(result).isEqualTo("success")
    }

    @Test
    fun failure() {
        val throwable = assertThrows<Throwable> {
            retryForResult(attempts = 3) { error("failure") }
        }

        assertThat(throwable.message).isEqualTo("Failed after 3 attempts")
        assertThat(throwable.cause is IllegalStateException).isTrue()
    }

    @Test
    fun `failure then success`() {
        var attempt = 0
        val result = retryForResult(attempts = 3) {
            attempt++
            if (attempt <= 2) {
                error("failure")
            } else {
                "success"
            }
        }

        assertThat(result).isEqualTo("success")
    }
}
