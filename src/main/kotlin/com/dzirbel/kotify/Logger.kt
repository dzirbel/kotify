package com.dzirbel.kotify

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.Logger.Event
import com.dzirbel.kotify.Logger.Network.intercept
import com.dzirbel.kotify.cache.ImageCacheEvent
import com.dzirbel.kotify.ui.framework.Presenter
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
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.statements.expandArgs
import kotlin.time.Duration
import kotlin.time.measureTimedValue

/**
 * A simple in-memory log of [Event]s storing arbitrary data of type [T], which can be [log]ed variously throughout the
 * application and retrieved to be exposed in the UI by [eventsFlow].
 */
@Stable
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

    protected fun log(
        title: String,
        content: String? = null,
        data: T,
        type: Event.Type = Event.Type.INFO,
        time: Long = System.currentTimeMillis(),
    ) {
        val event = Event(title = title, content = content, data = data, type = type, time = time)
        synchronized(events) {
            events.add(event)
            mutableEventsFlow.tryEmit(ArrayList(events))
        }
    }

    fun clear() {
        synchronized(events) {
            events.clear()
        }

        mutableEventsFlow.tryEmit(emptyList())
    }

    object Events : Logger<Unit>() {
        fun info(title: String, content: String? = null) {
            log(title = title, content = content, type = Event.Type.INFO, data = Unit)
        }

        fun warn(title: String, content: String? = null) {
            log(title = title, content = content, type = Event.Type.WARNING, data = Unit)
        }
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
                title = ">> ${request.method} ${request.url}",
                content = request.headers.toContentString(),
                data = EventData(
                    isSpotifyApi = request.url.host == "api.spotify.com",
                    isRequest = true,
                    isResponse = false,
                ),
            )
        }

        private fun httpResponse(response: Response, duration: Duration) {
            log(
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
                ),
            )
        }
    }

    object Database : Logger<Database.EventType>(), SqlLogger, StatementInterceptor {
        enum class EventType {
            STATEMENT, TRANSACTION
        }

        // map from transaction id to (name, statements)
        private val transactionMap = mutableMapOf<String, Pair<String?, MutableList<StatementContext>>>()
        private val closedTransactions = mutableSetOf<String>()
        private var transactionCount: Int = 0

        fun registerTransaction(transaction: Transaction, name: String?) {
            transaction.registerInterceptor(this)
            transactionMap[transaction.id] = Pair(name, mutableListOf())
        }

        override fun beforeCommit(transaction: Transaction) {
            closedTransactions.add(transaction.id)
            transactionCount++
            transactionMap.remove(transaction.id)?.let { (name, statements) ->
                val transactionTitle = name ?: transaction.id
                val transactionData = "Transaction #$transactionCount : $transactionTitle"
                log(
                    title = "$transactionData (${statements.size}) [${transaction.duration}ms]",
                    content = statements.joinToString(separator = "\n\n") { it.expandArgs(transaction) },
                    data = EventType.TRANSACTION,
                )
            }
        }

        override fun log(context: StatementContext, transaction: Transaction) {
            require(transaction.id !in closedTransactions) { "transaction ${transaction.id} was already closed" }

            val data = transactionMap[transaction.id]
            data?.second?.add(context)

            val tables = context.statement.targets.joinToString { it.tableName }
            val transactionTitle = data?.first ?: transaction.id
            val transactionData = "#${transaction.statementCount} in $transactionTitle"

            log(
                title = "$transactionData : ${context.statement.type} $tables [${transaction.duration}ms]",
                content = context.expandArgs(transaction),
                data = EventType.STATEMENT,
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

            log(title = title, type = type, data = Unit)
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
                title = "[$presenterClass] ERROR ${throwable::class.simpleName} : ${throwable.message}",
                content = throwable.stackTraceToString(),
                type = Event.Type.WARNING,
                data = EventData(presenterClass = presenterClass, type = EventType.ERROR),
            )
        }

        fun handleState(presenter: Presenter<*, *>, state: Any, stateCount: Int) {
            val presenterClass = presenter::class.simpleName
            log(
                title = "[$presenterClass] STATE #$stateCount ${state::class.simpleName}",
                content = state.toString(),
                data = EventData(presenterClass = presenterClass, type = EventType.STATE),
            )
        }

        fun handleEvent(presenter: Presenter<*, *>, event: Any, eventCount: Int) {
            val presenterClass = presenter::class.simpleName
            log(
                title = "[$presenterClass] EVENT #$eventCount ${event::class.simpleName}",
                content = event.toString(),
                data = EventData(presenterClass = presenterClass, type = EventType.EVENT),
            )
        }
    }
}
