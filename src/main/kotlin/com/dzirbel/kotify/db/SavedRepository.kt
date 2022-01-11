package com.dzirbel.kotify.db

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.dzirbel.kotify.db.model.GlobalUpdateTimesRepository
import java.lang.ref.WeakReference
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages the state of entities which can be saved/unsaved from a user's library.
 *
 * Locally, individual records for each entity is saved in the [savedEntityTable] with the time the entire library was
 * synced saved in the [GlobalUpdateTimesRepository] via [libraryUpdateKey].
 *
 * TODO split into interface (containing function definitions, some defaults, Listener, etc, but no db references)
 */
abstract class SavedRepository<SavedNetworkType>(
    private val savedEntityTable: SavedEntityTable,
    private val libraryUpdateKey: String = savedEntityTable.tableName,
) {
    private val listeners = mutableListOf<Listener>()

    private val states = ConcurrentHashMap<String, WeakReference<MutableState<Boolean?>>>()

    interface Listener {
        data class QueryEvent(val id: String, val result: Boolean?)

        /**
         * Invoked when cached saved states are queried, with [events] as the result.
         */
        fun onQueryCached(events: List<QueryEvent>) {}

        /**
         * Invoked when remote saved states are queried, with [events] as the result.
         */
        fun onQueryRemote(events: List<QueryEvent>) {}

        /**
         * Invoked when the saved state of [ids] is set to [saved].
         */
        fun onSetSaved(ids: List<String>, saved: Boolean) {}

        /**
         * Invoked when the cached saved library is invalidated.
         */
        fun onInvalidateLibrary() {}

        /**
         * Invoked when the cached saved library is queried, with [library] as the result.
         */
        fun onQueryLibraryCached(library: Set<String>?) {}

        /**
         * Invoked when the remote saved library is queried, with [library] as the result.
         */
        fun onQueryLibraryRemote(library: Set<String>) {}
    }

    init {
        // Use a Listener to keep states up-to-date with data from the remote. Is optimistic that cached values in the
        // database never change (i.e. onQueryCached is not used to update states).
        addListener(
            object : Listener {
                override fun onQueryRemote(events: List<Listener.QueryEvent>) {
                    events.forEach { event ->
                        states[event.id]?.get()?.value = event.result
                    }
                }

                override fun onSetSaved(ids: List<String>, saved: Boolean) {
                    ids.forEach { id ->
                        states[id]?.get()?.value = saved
                    }
                }

                override fun onQueryLibraryRemote(library: Set<String>) {
                    for ((id, reference) in states.entries) {
                        reference.get()?.value = library.contains(id)
                    }
                }
            }
        )
    }

    /**
     * Fetches the saved state of each of the given [ids] via a remote call to the network.
     *
     * This is the remote primitive and simply fetches the network state but does not cache it, unlike [isSavedRemote].
     */
    protected abstract suspend fun fetchIsSaved(ids: List<String>): List<Boolean>

    /**
     * Updates the saved state of each of the given [ids] to [saved] via a remote call to the network.
     *
     * This is the remote primitive and simply pushes the network state but does not cache it, unlike [setSaved].
     */
    protected abstract suspend fun pushSaved(ids: List<String>, saved: Boolean)

    /**
     * Fetches the current state of the library of saved entities, i.e. all the entities which the user has saved.
     *
     * This is the remote primitive and simply fetches the network state but does not cache it, unlike [getLibrary].
     */
    protected abstract suspend fun fetchLibrary(): Iterable<SavedNetworkType>

    /**
     * Converts the given [savedNetworkType] model into the ID of the saved entity, and adds any corresponding model to
     * the database. E.g. for saved artists, the attached artist model should be inserted into/used to update the
     * database and its ID returned. Always called from within a transaction.
     */
    protected abstract fun from(savedNetworkType: SavedNetworkType): String?

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    /**
     * Determines whether [id] has been saved to the user's library, from the local database cache. Returns null if its
     * status is not cached.
     */
    suspend fun isSavedCached(id: String): Boolean? = isSavedCached(ids = listOf(id))[0]

    /**
     * Determines whether each of [ids] has been saved to the user's library, from the local database cache. Returns
     * null for each if its status is not cached.
     */
    suspend fun isSavedCached(ids: List<String>): List<Boolean?> {
        return KotifyDatabase.transaction {
            ids.map { id -> savedEntityTable.isSaved(entityId = id) }
        }
            .also { results ->
                if (listeners.isNotEmpty()) {
                    val events = ids.zip(results) { id, result -> Listener.QueryEvent(id = id, result = result) }
                    listeners.forEach {
                        it.onQueryCached(events)
                    }
                }
            }
    }

    /**
     * Retrieves the saved state from the remote source for the given [id] without checking for a locally cached state,
     * saves it to the cache, and returns it.
     */
    suspend fun isSavedRemote(id: String): Boolean {
        val saved = fetchIsSaved(ids = listOf(id)).first()

        KotifyDatabase.transaction {
            savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = null)
        }

        if (listeners.isNotEmpty()) {
            val events = listOf(Listener.QueryEvent(id = id, result = saved))
            listeners.forEach {
                it.onQueryRemote(events)
            }
        }

        return saved
    }

    /**
     * Retrieves the saved state for the given [id], from the local cache if present, otherwise fetches it from the
     * remote source, caches, and returns it.
     */
    suspend fun isSaved(id: String): Boolean = isSavedCached(id) ?: isSavedRemote(id)

    /**
     * Returns a [State] reflecting the live saved state of the entity with the given [id].
     *
     * The returned [State] is the same object between calls for as long as it stays in context (i.e. is not
     * garbage-collected).
     *
     * If [fetchIfUnknown] is true, the saved state will be fetched from the remote state if it is currently unknown
     * (i.e. null). Otherwise, the returned state may be null.
     */
    suspend fun savedStateOf(id: String, fetchIfUnknown: Boolean = false): State<Boolean?> {
        states[id]?.get()?.let { cached ->
            // if the cached value is unknown, refresh directly from the remote (assuming that the cached value is
            // up-to-date with the cache already, so it can be skipped)
            if (fetchIfUnknown && cached.value == null) {
                isSavedRemote(id = id)
            }

            return cached
        }

        val saved = if (fetchIfUnknown) isSaved(id) else isSavedCached(id)
        val state = mutableStateOf(saved)
        states[id] = WeakReference(state)
        return state
    }

    /**
     * Clears the cache of states used by [savedStateOf], for use in tests.
     */
    fun clearStates() {
        states.clear()
    }

    /**
     * Saves the entity with the given [id] to the user's library, both via a remote call and in the local cache.
     */
    suspend fun save(id: String) = setSaved(id = id, saved = true)

    /**
     * Removes the entity with the given [id] from the user's library, both via a remote call and in the local cache.
     */
    suspend fun unsave(id: String) = setSaved(id = id, saved = false)

    /**
     * Saves the entities with the given [ids] to the user's library, both via a remote call and in the local cache.
     */
    suspend fun save(ids: List<String>) = setSaved(ids = ids, saved = true)

    /**
     * Removes the entities with the given [ids] from the user's library, both via a remote call and in the local cache.
     */
    suspend fun unsave(ids: List<String>) = setSaved(ids = ids, saved = false)

    /**
     * Adds or removes the entity with the given [id] from the user's library according to the given [saved] state, both
     * via a remote call and in the local cache.
     */
    suspend fun setSaved(id: String, saved: Boolean) = setSaved(ids = listOf(id), saved = saved)

    /**
     * Adds or removes the entities with the given [ids] from the user's library according to the given [saved] state,
     * both via a remote call and in the local cache.
     */
    suspend fun setSaved(ids: List<String>, saved: Boolean) {
        pushSaved(ids = ids, saved = saved)

        KotifyDatabase.transaction {
            ids.forEach { id -> savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = Instant.now()) }
        }

        listeners.forEach {
            it.onSetSaved(ids = ids, saved = saved)
        }
    }

    /**
     * Returns the last time the entire library state (i.e. set of all the user's saved entities) was updated, or null
     * if it has never been fetched.
     */
    suspend fun libraryUpdated(): Instant? {
        return KotifyDatabase.transaction { GlobalUpdateTimesRepository.updated(libraryUpdateKey) }
    }

    /**
     * Invalidates the library state, i.e. the set of all the user's saved entities, in the local cache.
     */
    suspend fun invalidateLibrary() {
        KotifyDatabase.transaction { GlobalUpdateTimesRepository.invalidate(libraryUpdateKey) }

        listeners.forEach {
            it.onInvalidateLibrary()
        }
    }

    /**
     * Gets the library of saved entity IDs from the local database cache, or null if it has never been fetched in full.
     */
    suspend fun getLibraryCached(): Set<String>? {
        return KotifyDatabase.transaction {
            if (GlobalUpdateTimesRepository.hasBeenUpdated(libraryUpdateKey)) {
                savedEntityTable.savedEntityIds()
            } else {
                null
            }
        }
            .also { library ->
                listeners.forEach {
                    it.onQueryLibraryCached(library = library)
                }
            }
    }

    /**
     * Gets the library of saved entity IDs from the remote network, without checking its state in the local database
     * cache.
     */
    suspend fun getLibraryRemote(): Set<String> {
        val savedNetworkModels = fetchLibrary()
        return KotifyDatabase.transaction {
            GlobalUpdateTimesRepository.setUpdated(libraryUpdateKey)
            savedNetworkModels.mapNotNullTo(mutableSetOf()) { from(it) }
        }
            .also { library ->
                listeners.forEach {
                    it.onQueryLibraryRemote(library = library)
                }
            }
    }

    /**
     * Retrieves the library of saved entity IDs, from the database cache if it is present or from the remote network
     * otherwise.
     */
    suspend fun getLibrary(): Set<String> = getLibraryCached() ?: getLibraryRemote()
}
