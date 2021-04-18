package com.dzirbel.kotify.network.oauth

import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Wraps a simple [ApplicationEngine] ktor server which responds to GET requests on the given [port] to capture
 * redirects from Spotify's OAuth request.
 *
 * TODO try again to test (couldn't get the test environment to use the right port)
 */
class LocalOAuthServer(
    port: Int = DEFAULT_PORT,
    private val state: String,
    private val callback: suspend (Result) -> Unit
) {
    private val server = embeddedServer(Netty, port = port) {
        routing {
            get {
                val state = call.parameters["state"]
                val error = call.parameters["error"]
                val code = call.parameters["code"]

                when (val result = handle(state = state, error = error, code = code)) {
                    is Result.Error -> call.respondText("Error: ${result.error}")
                    is Result.MismatchedState -> call.respondText(
                        "Mismatched state! Expected ${result.expectedState}; got ${result.actualState}"
                    )
                    is Result.Success -> call.respondText(
                        "Success! You can now close this tab.\n\nCode: ${result.code}"
                    )
                }
            }
        }
    }

    /**
     * Starts this [LocalOAuthServer] and returns it.
     */
    fun start(): LocalOAuthServer {
        server.start(wait = false)
        return this
    }

    /**
     * Stops this [LocalOAuthServer] and returns it.
     */
    fun stop(): LocalOAuthServer {
        server.stop(gracePeriodMillis = STOP_GRACE_PERIOD_MS, timeoutMillis = STOP_TIMEOUT_MS)
        return this
    }

    suspend fun manualRedirectUrl(url: String) {
        val httpUrl = url.toHttpUrlOrNull()
        val state = httpUrl?.queryParameter("state")
        val error = httpUrl?.queryParameter("error")
        val code = httpUrl?.queryParameter("code")

        handle(state = state, error = error, code = code)
    }

    private suspend fun handle(state: String?, error: String?, code: String?): Result {
        val result = if (state != this.state) {
            Result.MismatchedState(expectedState = this.state, actualState = state)
        } else {
            if (error == null && code != null) {
                Result.Success(code = code)
            } else {
                Result.Error(error = error)
            }
        }

        callback(result)
        return result
    }

    sealed class Result {
        class Error(val error: String?) : Result()
        class MismatchedState(val expectedState: String, val actualState: String?) : Result()
        class Success(val code: String) : Result()
    }

    companion object {
        // must be whitelisted as part of the redirect URL in the Spotify developer dashboard
        const val DEFAULT_PORT = 12_582

        private const val STOP_GRACE_PERIOD_MS = 1_000L
        private const val STOP_TIMEOUT_MS = 3_000L

        fun redirectUrl(port: Int): String = "http://localhost:$port/"
    }
}
