package com.dominiczirbel.network.oauth

import com.google.common.truth.BooleanSubject
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// TODO test requireRefreshable
// TODO test refresh
internal class AccessTokenTest {
    @BeforeEach
    @Suppress("unused")
    fun before() {
        AccessToken.Cache.clear()
    }

    @Test
    fun testIsExpired() {
        fun assertIsExpired(receivedDeltaMs: Long, expiresInS: Long): BooleanSubject {
            return assertThat(
                AccessToken(received = System.currentTimeMillis() + receivedDeltaMs, expiresIn = expiresInS).isExpired
            )
        }

        // received now, expires immediately -> expired
        assertIsExpired(receivedDeltaMs = 0, expiresInS = 0).isTrue()

        // received 100ms in the future, expires 1 seconds after received -> not expired
        assertIsExpired(receivedDeltaMs = 100, expiresInS = 1).isFalse()

        // received 100ms ago, expires 1 seconds after received -> not expired
        assertIsExpired(receivedDeltaMs = -100, expiresInS = 1).isFalse()

        // received 10s ago, expires 5 seconds after received -> expired
        assertIsExpired(receivedDeltaMs = -10_000, expiresInS = 5).isTrue()

        // received 10s ago, expires 15 seconds after received -> not expired
        assertIsExpired(receivedDeltaMs = -10_000, expiresInS = 15).isFalse()
    }

    @Test
    fun testGetPutClear() {
        assertThat(AccessToken.Cache.hasToken).isFalse()
        assertThat(runBlocking { AccessToken.Cache.get() }).isNull()
        assertThrows<AccessToken.Cache.NoAccessTokenError> { runBlocking { AccessToken.Cache.getOrThrow() } }

        val token1 = AccessToken(accessToken = "token1")
        AccessToken.Cache.put(token1)

        assertThat(AccessToken.Cache.hasToken).isTrue()
        assertThat(runBlocking { AccessToken.Cache.get() }).isSameInstanceAs(token1)
        assertThat(runBlocking { AccessToken.Cache.getOrThrow() }).isSameInstanceAs(token1)

        val token2 = AccessToken(accessToken = "token2")
        AccessToken.Cache.put(token2)

        assertThat(AccessToken.Cache.hasToken).isTrue()
        assertThat(runBlocking { AccessToken.Cache.get() }).isSameInstanceAs(token2)
        assertThat(runBlocking { AccessToken.Cache.getOrThrow() }).isSameInstanceAs(token2)

        AccessToken.Cache.clear()
        assertThat(AccessToken.Cache.hasToken).isFalse()
        assertThat(runBlocking { AccessToken.Cache.get() }).isNull()
        assertThrows<AccessToken.Cache.NoAccessTokenError> { runBlocking { AccessToken.Cache.getOrThrow() } }
    }

    @Test
    fun testSaveLoad() {
        val token1 = AccessToken(accessToken = "token1")
        assertThat(token1.received).isGreaterThan(0)
        AccessToken.Cache.put(token1)

        AccessToken.Cache.reset()

        val loadedToken = runBlocking { AccessToken.Cache.get() }
        assertThat(loadedToken).isEqualTo(token1)
        assertThat(loadedToken).isNotSameInstanceAs(token1)
        assertThat(loadedToken?.received).isEqualTo(token1.received)
    }

    @Test
    fun testFromJsonNoReceived() {
        val gson = Gson()

        val before = System.currentTimeMillis()

        val accessToken = gson.fromJson(
            """
            {
                accessToken: abc,
                tokenType: def,
                expiresIn: 30
            }
            """.trimIndent(),
            AccessToken::class.java
        )

        val after = System.currentTimeMillis()

        assertThat(accessToken.accessToken).isEqualTo("abc")
        assertThat(accessToken.tokenType).isEqualTo("def")
        assertThat(accessToken.expiresIn).isEqualTo(30)
        assertThat(accessToken.received).isIn(before..after)
    }

    @Test
    fun testFromJsonWithReceived() {
        val gson = Gson()

        val accessToken = gson.fromJson(
            """
            {
                accessToken: abc,
                tokenType: def,
                expiresIn: 30,
                received: 123
            }
            """.trimIndent(),
            AccessToken::class.java
        )

        assertThat(accessToken.accessToken).isEqualTo("abc")
        assertThat(accessToken.tokenType).isEqualTo("def")
        assertThat(accessToken.expiresIn).isEqualTo(30)
        assertThat(accessToken.received).isEqualTo(123)
    }
}
