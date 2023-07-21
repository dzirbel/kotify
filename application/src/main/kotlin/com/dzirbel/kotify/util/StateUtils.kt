package com.dzirbel.kotify.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.TransactionReadOnlyCachedProperty

/**
 * Returns a [State] produced by an asynchronous [KotifyDatabase.transaction] with the given [transactionName] and
 * [statement] content.
 *
 * TODO extract to :ui-kotify - this causes compilation errors for unknown reasons
 */
@Composable
fun <E : SpotifyEntity, T : Any> E.produceTransactionState(
    transactionName: String,
    initialValue: T? = null,
    statement: E.() -> T?,
): State<T?> {
    return produceState(initialValue = initialValue, key1 = this.id.value) {
        if (initialValue == null) {
            value = KotifyDatabase.transaction(name = transactionName) { statement() }
        }
    }
}

/**
 * Returns a [State] produced by an asynchronous [KotifyDatabase.transaction] based on this
 * [TransactionReadOnlyCachedProperty].
 */
@Composable
fun <V> TransactionReadOnlyCachedProperty<V>.produceTransactionState(onLive: (V) -> Unit = {}): State<V?> {
    return produceState(initialValue = cachedOrNull, key1 = this) {
        if (cachedOrNull == null) {
            value = KotifyDatabase.transaction(name = transactionName) { live.also(onLive) }
        }
    }
}
