package com.dzirbel.kotify.repository

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.util.CurrentTime
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class CacheStrategyTest {
    data class TTLCase(
        val currentTime: Instant,
        val updateTime: Instant,
        val transientTTL: Duration?,
        val invalidTTL: Duration?,
        val validity: CacheStrategy.CacheValidity,
    )

    @ParameterizedTest
    @MethodSource
    fun ttl(case: TTLCase) {
        CurrentTime.mocked(case.currentTime.toEpochMilli()) {
            val ttl = CacheStrategy.TTL<Instant>(
                transientTTL = case.transientTTL,
                invalidTTL = case.invalidTTL,
                getUpdateTime = { it },
            )

            assertThat(ttl.validity(case.updateTime)).isEqualTo(case.validity)
        }
    }

    companion object {
        @JvmStatic
        fun ttl(): List<TTLCase> {
            return listOf(
                // past both transient and invalid TTL
                TTLCase(
                    currentTime = Instant.ofEpochMilli(100),
                    updateTime = Instant.ofEpochMilli(50),
                    transientTTL = 10.milliseconds,
                    invalidTTL = 10.milliseconds,
                    validity = CacheStrategy.CacheValidity.INVALID,
                ),
                // past transient TTL
                TTLCase(
                    currentTime = Instant.ofEpochMilli(100),
                    updateTime = Instant.ofEpochMilli(50),
                    transientTTL = 10.milliseconds,
                    invalidTTL = 100.milliseconds,
                    validity = CacheStrategy.CacheValidity.TRANSIENT,
                ),
                // past invalid TTL
                TTLCase(
                    currentTime = Instant.ofEpochMilli(100),
                    updateTime = Instant.ofEpochMilli(50),
                    transientTTL = 100.milliseconds,
                    invalidTTL = 10.milliseconds,
                    validity = CacheStrategy.CacheValidity.INVALID,
                ),
                // past transient TTL; null invalid TTL
                TTLCase(
                    currentTime = Instant.ofEpochMilli(100),
                    updateTime = Instant.ofEpochMilli(50),
                    transientTTL = 10.milliseconds,
                    invalidTTL = null,
                    validity = CacheStrategy.CacheValidity.TRANSIENT,
                ),
                // past invalid TTL; null transient TTL
                TTLCase(
                    currentTime = Instant.ofEpochMilli(100),
                    updateTime = Instant.ofEpochMilli(50),
                    transientTTL = null,
                    invalidTTL = 10.milliseconds,
                    validity = CacheStrategy.CacheValidity.INVALID,
                ),
                // past neither transient nor invalid TTL
                TTLCase(
                    currentTime = Instant.ofEpochMilli(100),
                    updateTime = Instant.ofEpochMilli(50),
                    transientTTL = 100.milliseconds,
                    invalidTTL = 100.milliseconds,
                    validity = CacheStrategy.CacheValidity.VALID,
                ),
                // both null transient and invalid TTL
                TTLCase(
                    currentTime = Instant.ofEpochMilli(100),
                    updateTime = Instant.ofEpochMilli(50),
                    transientTTL = null,
                    invalidTTL = null,
                    validity = CacheStrategy.CacheValidity.VALID,
                ),
            )
        }
    }
}
