package com.dzirbel.kotify

import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.log.Logging
import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.asLog
import com.dzirbel.kotify.log.info
import com.dzirbel.kotify.network.Spotify
import kotlinx.coroutines.GlobalScope
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import kotlin.time.Duration
import kotlin.time.measureTimedValue

object NetworkLogger : Logging<NetworkLogger.LogData> {
    data class LogData(val isSpotifyApi: Boolean, val isRequest: Boolean) {
        val isResponse: Boolean
            get() = !isRequest
    }

    private val mutableLog = MutableLog<LogData>("Network", GlobalScope, writeContentToLogFile = false)

    private val SPOTIFY_URL_HOST = Spotify.API_URL.toHttpUrl().host

    override val log: Log<LogData> = mutableLog.asLog()

    fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        httpRequest(request)

        val (response, duration) = measureTimedValue { chain.proceed(request) }
        httpResponse(response, duration)

        return response
    }

    private fun httpRequest(request: Request) {
        mutableLog.info(
            title = "${request.method} ${request.url}",
            content = request.headers.toContentString(),
            data = LogData(
                isSpotifyApi = request.url.host == SPOTIFY_URL_HOST,
                isRequest = true,
            ),
        )
    }

    private fun httpResponse(response: Response, duration: Duration) {
        mutableLog.log(
            Log.Event(
                title = "${response.request.method} ${response.request.url} : ${response.code}",
                content = buildString {
                    append("Message: ${response.message}")
                    if (response.headers.any()) {
                        appendLine()
                        appendLine()
                        append(response.headers.toContentString())
                    }
                },
                type = @Suppress("MagicNumber") when (response.code) {
                    in 100 until 200 -> Log.Event.Type.INFO
                    in 200 until 300 -> Log.Event.Type.SUCCESS
                    in 300 until 400 -> Log.Event.Type.INFO
                    in 400 until 500 -> Log.Event.Type.WARNING
                    in 500 until 600 -> Log.Event.Type.ERROR
                    else -> Log.Event.Type.ERROR
                },
                duration = duration,
                data = LogData(
                    isSpotifyApi = response.request.url.host == SPOTIFY_URL_HOST,
                    isRequest = false,
                ),
            ),
        )
    }

    private fun Headers.toContentString(): String {
        return joinToString(separator = "\n") { (name, value) -> "$name : $value" }
    }
}
