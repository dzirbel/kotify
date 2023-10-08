package com.dzirbel.kotify.util.time

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import java.time.Instant

class ParseInstantTest {
    @Test
    fun valid() {
        assertThat(parseInstantOrNull("2007-12-03T10:15:30.00Z")).isEqualTo(Instant.ofEpochMilli(1_196_676_930_000))
    }

    @Test
    fun invalid() {
        assertThat(parseInstantOrNull("")).isNull()
        assertThat(parseInstantOrNull("2007-12-03T10:15:30.00")).isNull()
    }
}
