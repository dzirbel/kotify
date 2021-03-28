package com.dominiczirbel.network.oauth

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
    private val onSuccess: suspend (String) -> Unit,
    private val onError: suspend (String?) -> Unit,
    private val onMismatchedState: suspend (String?) -> Unit
) {
    private val server = embeddedServer(Netty, port = port) {
        routing {
            get {
                val state = call.parameters["state"]
                val error = call.parameters["error"]
                val code = call.parameters["code"]

                val response = handle(state = state, error = error, code = code)

                call.respondText(response.message)
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

    private suspend fun handle(state: String?, error: String?, code: String?): Response {
        return if (state != this.state) {
            onMismatchedState(state)
            Response("Mismatched state! Expected ${this.state}; got $state")
        } else {
            if (error == null && code != null) {
                onSuccess(code)
                Response("Success! Code: $code")
            } else {
                onError(error)
                Response("Error: $error")
            }
        }
    }

    data class Response(val message: String)

    companion object {
        const val DEFAULT_PORT = 12_582

        private const val STOP_GRACE_PERIOD_MS = 1_000L
        private const val STOP_TIMEOUT_MS = 3_000L

        fun redirectUrl(port: Int): String = "http://localhost:$port/"
    }
}
