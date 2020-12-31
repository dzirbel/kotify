package com.dominiczirbel.network.oauth

import com.dominiczirbel.network.Spotify
import com.dominiczirbel.withSpotifyConfiguration
import com.google.common.io.Files
import com.google.common.truth.BooleanSubject
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.concurrent.TimeUnit

// TODO test requireRefreshable
internal class AccessTokenTest {
    @BeforeEach
    @Suppress("unused")
    fun beforeEach() {
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
        val before = System.currentTimeMillis()

        val accessToken = Spotify.gson.fromJson(
            """
            {
                access_token: abc,
                token_type: def,
                expires_in: 30
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
        val accessToken = Spotify.gson.fromJson(
            """
            {
                access_token: abc,
                token_type: def,
                expires_in: 30,
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

    @Test
    fun testRefresh() {
        val tokenBody =
            """
            {
                access_token: abc,
                token_type: def,
                expires_in: 30
            }
            """.trimIndent()

        withSpotifyConfiguration(
            Spotify.configuration.copy(
                oauthOkHttpClient = OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        Response.Builder()
                            .code(200)
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .message("OK")
                            .body(tokenBody.toResponseBody("text/plain".toMediaType()))
                            .build()
                    }
                    .build()
            )
        ) {
            val expiredToken = AccessToken(
                expiresIn = 10,
                received = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(15),
                refreshToken = "refresh"
            )
            assertThat(expiredToken.isExpired).isTrue()
            AccessToken.Cache.put(expiredToken)

            val newToken = runBlocking { AccessToken.Cache.get() }

            assertThat(newToken).isNotNull()
            assertThat(newToken).isNotEqualTo(expiredToken)
            assertThat(newToken!!.isExpired).isFalse()
            assertThat(newToken.accessToken).isEqualTo("abc")
            assertThat(newToken.tokenType).isEqualTo("def")
            assertThat(newToken.expiresIn).isEqualTo(30)

            assertThat(runBlocking { AccessToken.Cache.get() }).isEqualTo(newToken)
        }
    }

    companion object {
        private val tempFile = File("temp_access_token.json")

        @BeforeAll
        @JvmStatic
        @Suppress("unused")
        fun before() {
            if (AccessToken.Cache.file.exists()) {
                println("Moving ${AccessToken.Cache.file} to temp file $tempFile")
                Files.move(AccessToken.Cache.file, tempFile)
                AccessToken.Cache.log = false
            } else {
                println("${AccessToken.Cache.file} does not exist; skipping move to temp file")
            }
        }

        @AfterAll
        @JvmStatic
        @Suppress("unused")
        fun after() {
            if (tempFile.exists()) {
                println("Restoring ${AccessToken.Cache.file} from temp file $tempFile")
                Files.move(tempFile, AccessToken.Cache.file)
                AccessToken.Cache.log = true
            } else {
                println("Temp file $tempFile does not exist; skipping restore to cache file")
            }
        }
    }
}
