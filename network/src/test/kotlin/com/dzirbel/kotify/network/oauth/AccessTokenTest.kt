package com.dzirbel.kotify.network.oauth

import assertk.Assert
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import com.dzirbel.kotify.network.MockOkHttpClient
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.concurrent.TimeUnit

internal class AccessTokenTest {
    @BeforeEach
    fun beforeEach() {
        CurrentTime.mocked {
            AccessToken.Cache.clear()
        }
    }

    @Test
    fun testIsExpired() {
        fun assertIsExpired(receivedDeltaMs: Long, expiresInS: Long): Assert<Boolean> {
            return CurrentTime.mocked {
                assertThat(
                    AccessToken(
                        accessToken = "",
                        tokenType = "",
                        received = CurrentTime.millis + receivedDeltaMs,
                        expiresIn = expiresInS,
                    ).isExpired,
                )
            }
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
    fun testScopes() {
        val token = CurrentTime.mocked {
            AccessToken(
                accessToken = "",
                tokenType = "",
                expiresIn = 0,
                scope = "user-library-read USER-LIBRARY-MODIFY",
            )
        }

        assertThat(token.hasScope(OAuth.Scope.USER_LIBRARY_READ)).isTrue()
        assertThat(token.hasScope(OAuth.Scope.USER_LIBRARY_MODIFY)).isTrue()
        assertThat(token.hasScope(OAuth.Scope.APP_REMOTE_CONTROL)).isFalse()
    }

    @Test
    fun testGetPutClear() {
        CurrentTime.mocked {
            val client = MockOkHttpClient()

            assertNoToken(client = client)

            val token1 = AccessToken(accessToken = "token1", tokenType = "", expiresIn = 0)
            AccessToken.Cache.put(token1)

            assertThat(AccessToken.Cache.tokenFlow.value).isNotNull()
            assertThat(runBlocking { AccessToken.Cache.get(client = client) }).isSameInstanceAs(token1)
            assertThat(runBlocking { AccessToken.Cache.getOrThrow(client = client) }).isSameInstanceAs(token1)

            val token2 = AccessToken(accessToken = "token2", tokenType = "", expiresIn = 0)
            AccessToken.Cache.put(token2)

            assertThat(AccessToken.Cache.tokenFlow.value).isNotNull()
            assertThat(runBlocking { AccessToken.Cache.get(client = client) }).isSameInstanceAs(token2)
            assertThat(runBlocking { AccessToken.Cache.getOrThrow(client = client) }).isSameInstanceAs(token2)

            AccessToken.Cache.clear()
            assertNoToken(client = client)
        }
    }

    @RepeatedTest(5)
    fun testSaveLoad() {
        val client = MockOkHttpClient()

        CurrentTime.mocked {
            val token1 = AccessToken(accessToken = "token1", tokenType = "", expiresIn = 0)
            assertThat(token1.received).isGreaterThan(0)
            AccessToken.Cache.put(token1)

            AccessToken.Cache.reset()
            Thread.sleep(5)

            val loadedToken = runBlocking { AccessToken.Cache.get(client = client) }
            assertThat(loadedToken).isEqualTo(token1)
            assertThat(loadedToken).isNotSameInstanceAs(token1)
        }
    }

    @Test
    fun testFromJsonNoReceived() {
        val time = 123_456L

        val accessToken = CurrentTime.mocked(time) {
            Json.decodeFromString<AccessToken>(
                """
                    {
                        "access_token": "abc",
                        "token_type": "def",
                        "expires_in": 30
                    }
                """.trimIndent(),
            )
        }

        assertThat(accessToken.accessToken).isEqualTo("abc")
        assertThat(accessToken.tokenType).isEqualTo("def")
        assertThat(accessToken.expiresIn).isEqualTo(30)
        assertThat(accessToken.received).isEqualTo(time)
    }

    @Test
    fun testFromJsonWithReceived() {
        val accessToken = Json.decodeFromString<AccessToken>(
            """
                {
                    "access_token": "abc",
                    "token_type": "def",
                    "expires_in": 30,
                    "received": 123
                }
            """.trimIndent(),
        )

        assertThat(accessToken.accessToken).isEqualTo("abc")
        assertThat(accessToken.tokenType).isEqualTo("def")
        assertThat(accessToken.expiresIn).isEqualTo(30)
        assertThat(accessToken.received).isEqualTo(123)
    }

    @Test
    fun testRefresh() {
        val tokenBody = """
            {
                "access_token": "abc",
                "token_type": "def",
                "expires_in": 30
            }
        """.trimIndent()

        val client = MockOkHttpClient(responseBody = tokenBody.toResponseBody("text/plain".toMediaType()))
        val tokenReceivedTime = 123_456L

        val expiredToken = AccessToken(
            accessToken = "",
            tokenType = "",
            expiresIn = 10,
            received = tokenReceivedTime,
            refreshToken = "refresh",
        )

        CurrentTime.mocked(tokenReceivedTime + TimeUnit.SECONDS.toMillis(15)) {
            assertThat(expiredToken.isExpired).isTrue()

            AccessToken.Cache.put(expiredToken)

            runTest {
                val newToken = requireNotNull(AccessToken.Cache.get(client = client))

                assertThat(newToken).isNotEqualTo(expiredToken)
                assertThat(newToken.isExpired).isFalse()
                assertThat(newToken.accessToken).isEqualTo("abc")
                assertThat(newToken.tokenType).isEqualTo("def")
                assertThat(newToken.expiresIn).isEqualTo(30)

                assertThat(AccessToken.Cache.get(client = client)).isEqualTo(newToken)
            }
        }
    }

    @Test
    fun testRefreshError() {
        val client = MockOkHttpClient(responseCode = 500, responseMessage = "Internal server error")
        val tokenReceivedTime = 123_456L

        val expiredToken = AccessToken(
            accessToken = "",
            tokenType = "",
            expiresIn = 10,
            received = tokenReceivedTime,
            refreshToken = "refresh",
        )

        CurrentTime.mocked(tokenReceivedTime + TimeUnit.SECONDS.toMillis(15)) {
            assertThat(expiredToken.isExpired).isTrue()

            AccessToken.Cache.put(expiredToken)

            runTest {
                val newToken = AccessToken.Cache.get(client = client)

                assertThat(newToken).isNull()
                assertNoToken(client = client)
            }
        }
    }

    @RepeatedTest(3)
    fun testRefreshConcurrent() {
        val tokenBody = """
            {
                "access_token": "abc",
                "token_type": "def",
                "expires_in": 30
            }
        """.trimIndent()

        val client = MockOkHttpClient(
            responseBody = tokenBody.toResponseBody("text/plain".toMediaType()),
            delayMs = 100,
        )

        val tokenReceivedTime = 123_456L

        val expiredToken = AccessToken(
            accessToken = "",
            tokenType = "",
            expiresIn = 10,
            received = tokenReceivedTime,
            refreshToken = "refresh",
        )

        CurrentTime.mocked(tokenReceivedTime + TimeUnit.SECONDS.toMillis(15)) {
            assertThat(expiredToken.isExpired).isTrue()

            AccessToken.Cache.put(expiredToken)

            runTest {
                val request1 = async { AccessToken.Cache.get(client = client) }
                val request2 = async {
                    delay(50)
                    AccessToken.Cache.get(client = client)
                }

                val token1 = request1.await()
                val token2 = request2.await()

                runCurrent()

                assertThat(token1).isSameInstanceAs(token2)
                assertThat(client.requests).hasSize(1)
            }
        }
    }

    @Test
    fun testRequireRefreshable() {
        val client = MockOkHttpClient()

        CurrentTime.mocked {
            val notRefreshable = AccessToken(accessToken = "token", tokenType = "type", expiresIn = 10)
            assertThat(notRefreshable.refreshToken).isNull()

            AccessToken.Cache.put(notRefreshable)
            assertThat(AccessToken.Cache.tokenFlow.value).isNotNull()

            AccessToken.Cache.requireRefreshable()

            assertNoToken(client = client)

            val refreshable = AccessToken(
                accessToken = "token2",
                tokenType = "type",
                expiresIn = 10,
                refreshToken = "refresh",
            )
            assertThat(refreshable.refreshToken).isNotNull()

            AccessToken.Cache.put(refreshable)
            assertThat(AccessToken.Cache.tokenFlow.value).isNotNull()

            AccessToken.Cache.requireRefreshable()

            assertThat(AccessToken.Cache.tokenFlow.value).isNotNull()
        }
    }

    private fun assertNoToken(client: OkHttpClient) {
        assertThat(AccessToken.Cache.tokenFlow.value).isNull()
        assertThat(runBlocking { AccessToken.Cache.get(client = client) }).isNull()
        assertThrows<AccessToken.Cache.NoAccessTokenError> {
            runBlocking { AccessToken.Cache.getOrThrow(client = client) }
        }
    }

    companion object {
        private val tempFile = File("temp_access_token.json")
        private var originalCacheFile: File? = null

        @BeforeAll
        @JvmStatic
        fun before() {
            originalCacheFile = AccessToken.Cache.cacheFile
            AccessToken.Cache.cacheFile = tempFile
        }

        @AfterAll
        @JvmStatic
        fun after() {
            AccessToken.Cache.cacheFile = originalCacheFile
            originalCacheFile = null
        }
    }
}
