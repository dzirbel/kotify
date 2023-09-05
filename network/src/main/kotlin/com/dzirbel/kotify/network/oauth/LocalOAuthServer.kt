package com.dzirbel.kotify.network.oauth

import io.ktor.server.application.call
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.net.URI

/**
 * Wraps a simple [ApplicationEngine] ktor server which responds to GET requests on the given [port] to capture
 * redirects from Spotify's OAuth request.
 */
class LocalOAuthServer(
    port: Int = DEFAULT_PORT,
    private val state: String,
    private val callback: suspend (Result) -> Unit,
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
                        "Mismatched state! Expected ${result.expectedState}; got ${result.actualState}",
                    )
                    is Result.Success -> call.respondText(
                        "Success! You can now close this tab.\n\nCode: ${result.code}",
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
    fun stop(gracePeriodMillis: Long = STOP_GRACE_PERIOD_MS, timeoutMillis: Long = STOP_TIMEOUT_MS): LocalOAuthServer {
        server.stop(gracePeriodMillis = gracePeriodMillis, timeoutMillis = timeoutMillis)
        return this
    }

    suspend fun manualRedirectUri(uri: URI): Result {
        val queryParams = uri.query.split('&')
            .mapNotNull { param ->
                param.split('=')
                    .takeIf { it.size == 2 }
                    ?.let { it[0] to it[1] }
            }
            .toMap()

        val state = queryParams["state"]
        val error = queryParams["error"]
        val code = queryParams["code"]

        return handle(state = state, error = error, code = code)
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
