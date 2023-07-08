package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles logic to load, cache, and update a key-value repository with String (ID) keys and values of type [T].
 *
 * This is the entrypoint for most UI elements to access data. In particular, the state of an entity is exposed via
 * [stateOf] which returns a [StateFlow] to which the UI can bind for a live view of the data.
 *
 * TODO polish documentation
 */
interface Repository<T> {
    /**
     * The [CacheStrategy] applied by default when loading remote data.
     */
    val defaultCacheStrategy: CacheStrategy<T>
        get() = CacheStrategy.AlwaysValid()

    /**
     * Retrieves a [StateFlow] which reflects the live [CacheState] of the entity with the given [id].
     *
     * This is a cheap call which returns the same [StateFlow] instance when possible. In particular, it does not
     * synchronously or asynchronously fetch cached or remote data for [id]; to ensure that the [StateFlow] is populated
     * asynchronously this is often paired with [ensureLoaded].
     *
     * The returned [StateFlow] has a null value initially, which is populated with the various [CacheState] states as
     * it is loaded.
     */
    fun stateOf(id: String): StateFlow<CacheState<T>?>

    /**
     * Asynchronously fetches data from the local and/or remote data sources for the given [id] if it has not yet been
     * loaded into the cache.
     */
    fun ensureLoaded(id: String, cacheStrategy: CacheStrategy<T> = defaultCacheStrategy)

    /**
     * Asynchronously fetches data from the local and/or remote data sources for each of the given [ids] if it has not
     * yet been loaded into the cache.
     */
    fun ensureLoaded(ids: Iterable<String>, cacheStrategy: CacheStrategy<T> = defaultCacheStrategy)

    /**
     * Forces the entity associated with the given [id] to be fetched from the remote data source.
     */
    fun refreshFromRemote(id: String) = ensureLoaded(id = id, cacheStrategy = CacheStrategy.NeverValid())

    companion object {
        /**
         * The [CoroutineScope] under which [Repository] (and [SavedRepository], etc) operations are run.
         *
         * Since repositories exist outside the scope of any particular UI element and their operations should not be
         * tied to them, this is typically the [GlobalScope]. For example, if navigating away from a page while its
         * content is still loading, this should not cancel the loading operation in case it can be used later (e.g. if
         * the user navigates to that page again).
         *
         * TODO restrict scope to test execution in unit tests
         */
        internal var scope: CoroutineScope = GlobalScope
            private set

        suspend fun withRepositoryScope(scope: CoroutineScope, block: suspend () -> Unit) {
            this.scope = scope
            block()
            this.scope = GlobalScope
        }
    }
}
