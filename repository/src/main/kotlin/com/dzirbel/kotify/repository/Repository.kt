package com.dzirbel.kotify.repository

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles logic to load, cache, and update a key-value repository with String (ID) keys and values of type [T].
 *
 * This is the entrypoint for most UI elements to access data. In particular, the state of an entity is exposed via
 * [stateOf] which returns a [StateFlow] to which the UI can bind for a live view of the data.
 */
@Stable
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
         * Whether access to repository implementations is allowed; defaults to false to prevent access from tests which
         * should not access repositories (such as screenshot tests where repository responses should be mocked).
         *
         * Applies to [Repository] implementations as well as [SavedRepository] and [RatingRepository].
         */
        var enabled: Boolean = false

        /**
         * The default [CoroutineScope] used for repository actions which should persist as long as the application,
         * e.g. fetching a resource which could be used on multiple screens.
         */
        val applicationScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

        /**
         * The default [CoroutineScope] used for repository actions which should be cancelled when the current user is
         * signed out.
         *
         * Should not be used when unauthenticated (i.e. the scope is not cancelled on sign in).
         */
        val userSessionScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

        /**
         * Throws an [IllegalStateException] if [enabled] is not true.
         */
        internal fun checkEnabled() {
            check(enabled) { "illegal repository access" }
        }
    }
}
