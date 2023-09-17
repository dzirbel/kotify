package com.dzirbel.kotify

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CLIArgumentsTest {
    @Test
    fun parse() {
        val args = CLIArguments.parse(
            arrayOf("--cache-dir", "cache", "--settings-dir", "settings", "--log-dir", "log", "--debug"),
        )

        assertThat(args.cacheDir).isEqualTo("cache")
        assertThat(args.settingsDir).isEqualTo("settings")
        assertThat(args.logDir).isEqualTo("log")
        assertThat(args.debug).isTrue()
    }

    @Test
    fun parseUnknown() {
        assertThrows<Throwable> { CLIArguments.parse(arrayOf("dne")) }
    }

    @Test
    fun parseEmpty() {
        val args = CLIArguments.parse(emptyArray())

        assertThat(args.cacheDir).isNull()
        assertThat(args.settingsDir).isNull()
        assertThat(args.logDir).isNull()
        assertThat(args.debug).isFalse()
    }
}
