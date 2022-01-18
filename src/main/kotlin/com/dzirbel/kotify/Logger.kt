package com.dzirbel.kotify

import com.dzirbel.kotify.Logger.Event
import com.dzirbel.kotify.Logger.Network.intercept
import com.dzirbel.kotify.cache.ImageCacheEvent
import com.dzirbel.kotify.ui.Presenter
import com.dzirbel.kotify.util.ellipsize
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * A simple in-memory log of [Event]s, which can be [log]ed variously throughout the application and retrieved to be
 * exposed in the UI by [events] and [eventsFlow].
 */
sealed class Logger(private val tag: String) {
    /**
     * A single event that can be logged.
     */
    data class Event(
        val message: String,
        val type: Type = Type.INFO,
        val time: Long = System.currentTimeMillis(),
    ) {
        enum class Type {
            INFO, SUCCESS, WARNING, ERROR
        }
    }

    private val _events = mutableListOf<Event>()
    val events: List<Event>
        get() = _events.toList()

    private val _eventsFlow = MutableSharedFlow<List<Event>>()
    val eventsFlow = _eventsFlow.asSharedFlow()

    protected fun log(lazyEvents: () -> List<Event>) {
        GlobalScope.launch {
            val newEvents = lazyEvents()

            val allEvents = synchronized(_events) {
                _events.addAll(0, newEvents)
                _events.toList()
            }

            _eventsFlow.emit(allEvents)

            if (logToConsole) {
                newEvents.forEach { event ->
                    println("$tag ${event.message}")
                }
            }
        }
    }

    fun clear() {
        GlobalScope.launch {
            synchronized(_events) {
                _events.clear()
            }

            _eventsFlow.emit(listOf())
        }
    }

    /**
     * A global [Logger] which can [intercept] OkHttp requests and log events for each of them.
     */
    object Network : Logger("NETWORK") {
        fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            httpRequest(request)

            val (response, duration) = measureTimedValue { chain.proceed(request) }
            httpResponse(response, duration)

            return response
        }

        private fun httpRequest(request: Request) {
            log { listOf(Event(">> ${request.method} ${request.url}")) }
        }

        private fun httpResponse(response: Response, duration: Duration) {
            log { listOf(Event("<< ${response.code} ${response.request.method} ${response.request.url} in $duration")) }
        }
    }

    /**
     * A global [Logger] which logs events from the [com.dzirbel.kotify.cache.SpotifyImageCache].
     */
    object ImageCache : Logger("IMAGE CACHE") {
        fun handleImageCacheEvent(imageCacheEvent: ImageCacheEvent) {
            log {
                val message = when (imageCacheEvent) {
                    is ImageCacheEvent.InMemory -> "IN-MEMORY ${imageCacheEvent.url}"
                    is ImageCacheEvent.OnDisk ->
                        "ON-DISK ${imageCacheEvent.url} as ${imageCacheEvent.cacheFile} " +
                            "(loaded file in ${imageCacheEvent.duration})"
                    is ImageCacheEvent.Fetch ->
                        "MISS ${imageCacheEvent.url} in ${imageCacheEvent.duration}" +
                            imageCacheEvent.cacheFile?.let { " (saved to $it)" }
                }

                val type = when (imageCacheEvent) {
                    is ImageCacheEvent.InMemory -> Event.Type.SUCCESS
                    is ImageCacheEvent.OnDisk -> Event.Type.INFO
                    is ImageCacheEvent.Fetch -> Event.Type.WARNING
                }

                listOf(Event(message = message, type = type))
            }
        }
    }

    object UI : Logger("UI") {
        private const val MAX_STATE_LENGTH = 200

        fun handleError(presenter: Presenter<*, *>, throwable: Throwable) {
            if (logToConsole) {
                throwable.printStackTrace()
            }

            log {
                listOf(
                    Event(
                        message = "[${presenter::class.simpleName}] Error -> $throwable",
                        type = Event.Type.WARNING
                    )
                )
            }
        }

        fun handleState(presenter: Presenter<*, *>, state: Any) {
            log {
                listOf(
                    Event(
                        message = "[${presenter::class.simpleName}] State -> " +
                            state.toString().ellipsize(MAX_STATE_LENGTH)
                    )
                )
            }
        }

        fun handleEvent(presenter: Presenter<*, *>, event: Any) {
            log {
                listOf(Event(message = "[${presenter::class.simpleName}] Event -> $event"))
            }
        }
    }

    companion object {
        var logToConsole = false
    }
}
