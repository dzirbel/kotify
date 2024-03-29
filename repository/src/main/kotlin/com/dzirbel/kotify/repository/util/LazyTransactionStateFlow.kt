package com.dzirbel.kotify.repository.util

import com.dzirbel.kotify.db.DB
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow.Companion.requestBatched
import com.dzirbel.kotify.util.collections.zipEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [StateFlow] which lazily loads its value from the database on first access.
 *
 * This allows loading a value associated with a model or view model from a database transaction, with the following
 * properties:
 * - lazy: only loaded on the first collection of the flow
 * - observable: the loaded value can be observed as a [StateFlow] (e.g. collected into a Compose State)
 * - batched: multiple properties can be loaded in a single transaction via [requestBatched]
 */
class LazyTransactionStateFlow<T>(
    private val transactionName: String,
    initialValue: T? = null,
    requested: Boolean = initialValue != null,
    private val db: DB = DB.CACHE,
    private val scope: CoroutineScope = Repository.applicationScope,
    private val getter: () -> T?,
) : StateFlow<T?> {
    private val requested = AtomicBoolean(requested)
    private val flow = MutableStateFlow(initialValue)

    override val replayCache: List<T?>
        get() = flow.replayCache

    override val value: T?
        get() = flow.value

    /**
     * Creates a [LazyTransactionStateFlow] from a given [value], which will prevent it from ever being retrieved from
     * the database.
     */
    constructor(value: T?) : this(
        transactionName = "should not be used",
        initialValue = value,
        requested = true,
        getter = { error("should not be called") },
    )

    override suspend fun collect(collector: FlowCollector<T?>): Nothing {
        if (!requested.getAndSet(true)) {
            scope.launch {
                flow.value = KotifyDatabase[db].transaction(name = transactionName) { getter() }
            }
        }

        flow.collect(collector)
    }

    companion object {
        /**
         * Initiates loading for the [LazyTransactionStateFlow]s produced by [extractor] on this [Iterable], with values
         * loaded in a single batched transaction.
         */
        fun <T, R : Any> Iterable<T>.requestBatched(
            transactionName: (Int) -> String,
            db: DB = DB.CACHE,
            scope: CoroutineScope = Repository.applicationScope,
            extractor: (T) -> LazyTransactionStateFlow<R>,
        ) {
            val unrequestedProperties = mapNotNull { element ->
                extractor(element).takeIf { !it.requested.getAndSet(true) }
            }

            if (unrequestedProperties.isNotEmpty()) {
                scope.launch {
                    val values = KotifyDatabase[db].transaction(name = transactionName(unrequestedProperties.size)) {
                        unrequestedProperties.map { property ->
                            // use with{} to include both property and transaction as receiver params
                            with(property) { getter() }
                        }
                    }

                    unrequestedProperties.zipEach(values) { property, value ->
                        property.flow.value = value
                    }
                }
            }
        }
    }
}
