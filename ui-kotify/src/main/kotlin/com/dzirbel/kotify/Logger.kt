package com.dzirbel.kotify

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.Logger.Event
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.ui.ImageCacheEvent
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.statements.expandArgs

/**
 * A simple in-memory log of [Event]s storing arbitrary data of type [T], which can be [log]ed variously throughout the
 * application and retrieved to be exposed in the UI by [eventsFlow].
 *
 * TODO rework logging
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
        val time: Long = CurrentTime.millis,
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
        time: Long = CurrentTime.millis,
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

    object Database :
        Logger<Database.EventType>(),
        KotifyDatabase.TransactionListener,
        SqlLogger,
        StatementInterceptor {

        enum class EventType {
            STATEMENT, TRANSACTION
        }

        // map from transaction id to (name, statements)
        private val transactionMap = mutableMapOf<String, Pair<String?, MutableList<StatementContext>>>()
        private val closedTransactions = mutableSetOf<String>()
        private var transactionCount: Int = 0

        override fun onTransactionStart(transaction: Transaction, name: String?) {
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

    object UI : Logger<Unit>()
}
