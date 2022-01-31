package com.dzirbel.kotify

import com.dzirbel.kotify.Logger.Event
import com.dzirbel.kotify.Logger.Network.intercept
import com.dzirbel.kotify.cache.ImageCacheEvent
import com.dzirbel.kotify.ui.Presenter
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * A simple in-memory log of [Event]s storing arbitrary data of type [T], which can be [log]ed variously throughout the
 * application and retrieved to be exposed in the UI by [eventsFlow].
 */
sealed class Logger<T> {
    /**
     * A single event that can be logged.
     */
    data class Event<T>(
        val title: String,
        val content: String? = null,
        val data: T,
        val type: Type = Type.INFO,
        val time: Long = System.currentTimeMillis(),
    ) {
        enum class Type {
            INFO, SUCCESS, WARNING, ERROR
        }
    }

    private val events = mutableListOf<Event<T>>()
    private val mutableEventsFlow = MutableSharedFlow<List<Event<T>>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * A [SharedFlow] view of the events held by this logged. Replays the current list of events to new subscribers.
     */
    val eventsFlow: SharedFlow<List<Event<T>>>
        get() = mutableEventsFlow.asSharedFlow()

    protected fun log(newEvent: Event<T>) {
        synchronized(events) {
            events.add(newEvent)
            mutableEventsFlow.tryEmit(ArrayList(events))
        }
    }

    fun clear() {
        synchronized(events) {
            events.clear()
        }

        mutableEventsFlow.tryEmit(emptyList())
    }

    /**
     * A global [Logger] which can [intercept] OkHttp requests and log events for each of them.
     */
    object Network : Logger<Network.EventData>() {
        data class EventData(val isSpotifyApi: Boolean, val isRequest: Boolean, val isResponse: Boolean)

        private fun Headers.toContentString(): String {
            val headers = this
            return buildString {
                headers.forEach { (name, value) ->
                    append("$name : $value")
                    appendLine()
                }
            }
        }

        fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            httpRequest(request)

            val (response, duration) = measureTimedValue { chain.proceed(request) }
            httpResponse(response, duration)

            return response
        }

        private fun httpRequest(request: Request) {
            log(
                Event(
                    title = ">> ${request.method} ${request.url}",
                    content = request.headers.toContentString(),
                    data = EventData(
                        isSpotifyApi = request.url.host == "api.spotify.com",
                        isRequest = true,
                        isResponse = false,
                    )
                )
            )
        }

        private fun httpResponse(response: Response, duration: Duration) {
            log(
                Event(
                    title = "<< ${response.code} ${response.request.method} ${response.request.url} in $duration",
                    content = buildString {
                        append("Message: ${response.message}")
                        if (response.headers.any()) {
                            appendLine()
                            appendLine()

                            append(response.headers.toContentString())
                        }
                    },
                    data = EventData(
                        isSpotifyApi = response.request.url.host == "api.spotify.com",
                        isRequest = false,
                        isResponse = true,
                    )
                )
            )
        }
    }

    object Database : Logger<Unit>(), SqlLogger {
        override fun log(context: StatementContext, transaction: Transaction) {
            val tables = context.statement.targets.joinToString { it.tableName }
            val transactionData = "#${transaction.statementCount} in ${transaction.id}"
            log(
                Event(
                    title = "$transactionData : ${context.statement.type} $tables [${transaction.duration}ms]",
                    content = context.expandArgs(transaction),
                    data = Unit,
                )
            )
        }
    }

    /**
     * A global [Logger] which logs events from the [com.dzirbel.kotify.cache.SpotifyImageCache].
     */
    object ImageCache : Logger<Unit>() {
        fun handleImageCacheEvent(imageCacheEvent: ImageCacheEvent) {
            val title = when (imageCacheEvent) {
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

            log(Event(title = title, type = type, data = Unit))
        }
    }

    object UI : Logger<UI.EventData>() {
        enum class EventType {
            ERROR, STATE, EVENT
        }

        data class EventData(val presenterClass: String?, val type: EventType)

        fun handleError(presenter: Presenter<*, *>, throwable: Throwable) {
            val presenterClass = presenter::class.simpleName
            log(
                Event(
                    title = "[$presenterClass] ERROR ${throwable::class.simpleName} : ${throwable.message}",
                    content = throwable.stackTraceToString(),
                    type = Event.Type.WARNING,
                    data = EventData(presenterClass = presenterClass, type = EventType.ERROR),
                )
            )
        }

        fun handleState(presenter: Presenter<*, *>, state: Any, stateCount: Int) {
            val presenterClass = presenter::class.simpleName
            log(
                Event(
                    title = "[$presenterClass] STATE #$stateCount ${state::class.simpleName}",
                    content = state.toString(),
                    data = EventData(presenterClass = presenterClass, type = EventType.STATE),
                )
            )
        }

        fun handleEvent(presenter: Presenter<*, *>, event: Any, eventCount: Int) {
            val presenterClass = presenter::class.simpleName
            log(
                Event(
                    title = "[$presenterClass] EVENT #$eventCount ${event::class.simpleName}",
                    content = event.toString(),
                    data = EventData(presenterClass = presenterClass, type = EventType.EVENT),
                )
            )
        }
    }
}
