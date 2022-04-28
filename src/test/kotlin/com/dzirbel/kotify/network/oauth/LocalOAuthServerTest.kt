package com.dzirbel.kotify.network.oauth

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class LocalOAuthServerTest {
    @ParameterizedTest
    @ValueSource(ints = [LocalOAuthServer.DEFAULT_PORT, LocalOAuthServer.DEFAULT_PORT + 1])
    fun testMismatchedState(port: Int) {
        val expectedState = "a1b2c3"
        val inputState = "wrong"
        lateinit var callbackResult: LocalOAuthServer.Result
        LocalOAuthServer(state = expectedState, port = port, callback = { callbackResult = it }).running {
            val response = runBlocking {
                HttpClient().get("http://localhost:$port?state=$inputState")
            }

            val content = runBlocking { response.body<String>() }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(content).isEqualTo("Mismatched state! Expected $expectedState; got $inputState")
            assertThat(callbackResult).isInstanceOf(LocalOAuthServer.Result.MismatchedState::class).all {
                prop(LocalOAuthServer.Result.MismatchedState::expectedState).isEqualTo(expectedState)
                prop(LocalOAuthServer.Result.MismatchedState::actualState).isEqualTo(inputState)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = [LocalOAuthServer.DEFAULT_PORT, LocalOAuthServer.DEFAULT_PORT + 1])
    fun testSuccess(port: Int) {
        val state = "a1b2c3"
        val code = "code"
        lateinit var callbackResult: LocalOAuthServer.Result
        LocalOAuthServer(state = state, port = port, callback = { callbackResult = it }).running {
            val response = runBlocking {
                HttpClient().get("http://localhost:$port?state=$state&code=$code")
            }

            val content = runBlocking { response.body<String>() }

            assertThat(response.status).isEqualTo(HttpStatusCode.OK)
            assertThat(content).isEqualTo("Success! You can now close this tab.\n\nCode: $code")
            assertThat(callbackResult).isInstanceOf(LocalOAuthServer.Result.Success::class).all {
                prop(LocalOAuthServer.Result.Success::code).isEqualTo(code)
            }
        }
    }

    private fun LocalOAuthServer.running(block: () -> Unit) {
        start()

        try {
            block()
        } finally {
            stop()
        }
    }
}
