package com.dzirbel.kotify.repository.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class BackoffStrategyTest {
    @Test
    fun ofDelays() {
        val strategy = BackoffStrategy.OfDelays(10, 20, 30, 40)
        assertThat(strategy.delayFor(attempt = -1)).isNull()
        assertThat(strategy.delayFor(attempt = 0)).isEqualTo(10.milliseconds)
        assertThat(strategy.delayFor(attempt = 1)).isEqualTo(20.milliseconds)
        assertThat(strategy.delayFor(attempt = 2)).isEqualTo(30.milliseconds)
        assertThat(strategy.delayFor(attempt = 3)).isEqualTo(40.milliseconds)
        assertThat(strategy.delayFor(attempt = 4)).isNull()
    }
}
