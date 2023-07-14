package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.repository.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles logic to load, cache, and update a key-value repository with String (ID) keys and values of type [T].
 *
 * This is the entrypoint for most UI elements to access data. In particular, the state of an entity is exposed via
 * [stateOf] which returns a [StateFlow] to which the UI can bind for a live view of the data.
 */
interface Repository<T> {
    /**
     * The [CacheStrategy] applied by default determine the validity of locally cached data.
     */
    val defaultCacheStrategy: CacheStrategy<T>
        get() = CacheStrategy.AlwaysValid()

    /**
     * Retrieves a [StateFlow] which reflects the live [CacheState] of the entity with the given [id].
     *
     * The returned [StateFlow] is generally the same instance across calls with the same [id], but if there are no
     * active external references to the [StateFlow] then it may be garbage collected and subsequent calls will return
     * a new instance. When a new [StateFlow] is created it will be asynchronously populated with data fetched first
     * from the local cache if present and valid according to [cacheStrategy] or from the remote data source.
     *
     * The returned [StateFlow] may have an initial null value, which is populated with the various [CacheState] states
     * as it is loaded.
     *
     * TODO initialize to refreshing state?
     */
    fun stateOf(id: String, cacheStrategy: CacheStrategy<T> = defaultCacheStrategy): StateFlow<CacheState<T>?>

    /**
     * Retrieves a batch of [StateFlow]s which reflect the respective live [CacheState] of the entities with the given
     * [ids].
     *
     * This is preferred for retrieving multiple states of the same type (e.g. the tracks on an album) since calls to
     * the local cache and/or the remote data source can be batched.
     *
     * @see stateOf
     */
    fun statesOf(
        ids: Iterable<String>,
        cacheStrategy: CacheStrategy<T> = defaultCacheStrategy,
    ): List<StateFlow<CacheState<T>?>>

    /**
     * Forces the entity associated with the given [id] to be fetched from the remote data source.
     */
    fun refreshFromRemote(id: String): Job

    companion object {
        /**
         * The [CoroutineScope] under which [Repository] (and [SavedRepository], etc) operations are run.
         *
         * Since repositories exist outside the scope of any particular UI element and their operations should not be
         * tied to them, this is typically the [GlobalScope]. For example, if navigating away from a page while its
         * content is still loading, this should not cancel the loading operation in case it can be used later (e.g. if
         * the user navigates to that page again).
         *
         * TODO automatically restrict scope to test execution in unit tests instead of [withRepositoryScope]
         */
        var scope: CoroutineScope = GlobalScope
            private set

        internal suspend fun withRepositoryScope(scope: CoroutineScope, block: suspend () -> Unit) {
            this.scope = scope
            block()
            this.scope = GlobalScope
        }
    }
}
