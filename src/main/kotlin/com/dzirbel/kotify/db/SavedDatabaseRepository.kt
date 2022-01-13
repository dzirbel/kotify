package com.dzirbel.kotify.db

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.dzirbel.kotify.cache.SavedRepository
import com.dzirbel.kotify.db.model.GlobalUpdateTimesRepository
import java.lang.ref.WeakReference
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * A [SavedRepository] which uses a database table [savedEntityTable] as its local cache for individual saved states an
 * the [GlobalUpdateTimesRepository] via [libraryUpdateKey] for the library update time.
 */
abstract class SavedDatabaseRepository<SavedNetworkType>(
    private val savedEntityTable: SavedEntityTable,
    private val libraryUpdateKey: String = savedEntityTable.tableName,
) : SavedRepository {
    private val listeners = mutableListOf<SavedRepository.Listener>()

    private val states = ConcurrentHashMap<String, WeakReference<MutableState<Boolean?>>>()

    init {
        // Use a Listener to keep states up-to-date with data from the remote. Is optimistic that cached values in the
        // database never change (i.e. onQueryCached is not used to update states).
        addListener(
            object : SavedRepository.Listener {
                override fun onQueryRemote(events: List<SavedRepository.Listener.QueryEvent>) {
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

    final override fun addListener(listener: SavedRepository.Listener) {
        listeners.add(listener)
    }

    final override suspend fun isSavedCached(ids: List<String>): List<Boolean?> {
        return KotifyDatabase.transaction {
            val hasFetchedLibrary = GlobalUpdateTimesRepository.hasBeenUpdated(libraryUpdateKey)
            // if we've fetched the entire library, then any saved entity not present in the table is unsaved (or was
            // when the library was fetched)
            val default = if (hasFetchedLibrary) false else null
            ids.map { id -> savedEntityTable.isSaved(entityId = id) ?: default }
        }
            .also { results ->
                if (listeners.isNotEmpty()) {
                    val events = ids.zip(results) { id, result ->
                        SavedRepository.Listener.QueryEvent(id = id, result = result)
                    }
                    listeners.forEach {
                        it.onQueryCached(events)
                    }
                }
            }
    }

    final override suspend fun isSavedRemote(id: String): Boolean {
        val saved = fetchIsSaved(ids = listOf(id)).first()

        KotifyDatabase.transaction {
            savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = null)
        }

        if (listeners.isNotEmpty()) {
            val events = listOf(SavedRepository.Listener.QueryEvent(id = id, result = saved))
            listeners.forEach {
                it.onQueryRemote(events)
            }
        }

        return saved
    }

    final override suspend fun savedStateOf(id: String, fetchIfUnknown: Boolean): State<Boolean?> {
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

    final override suspend fun setSaved(ids: List<String>, saved: Boolean) {
        pushSaved(ids = ids, saved = saved)

        KotifyDatabase.transaction {
            ids.forEach { id -> savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = Instant.now()) }
        }

        listeners.forEach {
            it.onSetSaved(ids = ids, saved = saved)
        }
    }

    final override suspend fun libraryUpdated(): Instant? {
        return KotifyDatabase.transaction { GlobalUpdateTimesRepository.updated(libraryUpdateKey) }
    }

    final override suspend fun invalidateLibrary() {
        KotifyDatabase.transaction { GlobalUpdateTimesRepository.invalidate(libraryUpdateKey) }

        listeners.forEach {
            it.onInvalidateLibrary()
        }
    }

    final override suspend fun getLibraryCached(): Set<String>? {
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

    final override suspend fun getLibraryRemote(): Set<String> {
        val savedNetworkModels = fetchLibrary()
        return KotifyDatabase.transaction {
            GlobalUpdateTimesRepository.setUpdated(libraryUpdateKey)
            savedNetworkModels.mapNotNullTo(mutableSetOf()) { from(it) }
                .onEach { id ->
                    savedEntityTable.setSaved(entityId = id, saved = true, savedTime = null)
                }
        }
            .also { library ->
                listeners.forEach {
                    it.onQueryLibraryRemote(library = library)
                }
            }
    }
}
