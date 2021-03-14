package com.dominiczirbel

import com.dominiczirbel.Logger.Event
import com.dominiczirbel.Logger.Network.intercept
import com.dominiczirbel.cache.CacheEvent
import com.dominiczirbel.cache.ImageCacheEvent
import com.dominiczirbel.network.model.SpotifyObject
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
        val time: Long = System.currentTimeMillis()
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
     * A global [Logger] which logs events from the [com.dominiczirbel.cache.SpotifyCache].
     */
    object Cache : Logger("CACHE") {
        fun handleCacheEvents(cacheEvents: List<CacheEvent>) {
            log {
                cacheEvents.map { cacheEvent ->
                    val message = when (cacheEvent) {
                        is CacheEvent.Load -> "LOAD from ${cacheEvent.file} in ${cacheEvent.duration}"
                        is CacheEvent.Save -> "SAVE to ${cacheEvent.file} in ${cacheEvent.duration}"
                        is CacheEvent.Dump -> "DUMP"
                        is CacheEvent.Clear -> "CLEAR"
                        is CacheEvent.Hit -> {
                            val nameSuffix = (cacheEvent.value.obj as? SpotifyObject)?.name?.let { " ($it)" }.orEmpty()
                            "HIT ${cacheEvent.id}: ${cacheEvent.value.type}" + nameSuffix
                        }
                        is CacheEvent.Miss -> "MISS ${cacheEvent.id}"
                        is CacheEvent.Update -> {
                            val previousSuffix = cacheEvent.previous?.type?.let { " (was $it)" }.orEmpty()
                            "PUT ${cacheEvent.id}: ${cacheEvent.new.type}" + previousSuffix
                        }
                        is CacheEvent.Invalidate -> "INVALIDATE ${cacheEvent.id} (was ${cacheEvent.value.type})"
                    }

                    val type = when (cacheEvent) {
                        is CacheEvent.Dump -> Event.Type.INFO
                        is CacheEvent.Clear -> Event.Type.WARNING
                        is CacheEvent.Hit -> Event.Type.SUCCESS
                        is CacheEvent.Invalidate -> Event.Type.INFO
                        is CacheEvent.Load -> Event.Type.INFO
                        is CacheEvent.Miss -> Event.Type.WARNING
                        is CacheEvent.Save -> Event.Type.INFO
                        is CacheEvent.Update -> Event.Type.INFO
                    }

                    Event(message = message, type = type)
                }
            }
        }
    }

    /**
     * A global [Logger] which logs events from the [com.dominiczirbel.cache.SpotifyImageCache].
     */
    object ImageCache : Logger("IMAGE CACHE") {
        fun handleImageCacheEvent(imageCacheEvent: ImageCacheEvent) {
            log {
                val message = when (imageCacheEvent) {
                    is ImageCacheEvent.Hit ->
                        "HIT ${imageCacheEvent.url} at ${imageCacheEvent.cacheFile} " +
                            "(loaded file in ${imageCacheEvent.loadDuration})"
                    is ImageCacheEvent.Miss -> "MISS ${imageCacheEvent.url}"
                    is ImageCacheEvent.Fetch -> {
                        val writeSuffix = imageCacheEvent.cacheFile?.let { cacheFile ->
                            imageCacheEvent.writeDuration?.let { writeDuration ->
                                " (written to $cacheFile in $writeDuration)"
                            }
                        }.orEmpty()
                        "FETCH ${imageCacheEvent.url} in ${imageCacheEvent.fetchDuration}" + writeSuffix
                    }
                }

                val type = when (imageCacheEvent) {
                    is ImageCacheEvent.Fetch -> Event.Type.INFO
                    is ImageCacheEvent.Hit -> Event.Type.SUCCESS
                    is ImageCacheEvent.Miss -> Event.Type.WARNING
                }

                listOf(Event(message = message, type = type))
            }
        }
    }

    companion object {
        var logToConsole = false
    }
}
