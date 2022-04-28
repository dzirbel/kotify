package com.dzirbel.kotify.db

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.dzirbel.kotify.db.model.GlobalUpdateTimesRepository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.util.plusOrMinus
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.jetbrains.exposed.sql.deleteAll
import java.time.Instant

/**
 * A [SavedRepository] which uses a database table [savedEntityTable] as its local cache for individual saved states an
 * the [GlobalUpdateTimesRepository] via [libraryUpdateKey] for the library update time.
 */
abstract class SavedDatabaseRepository<SavedNetworkType>(
    /**
     * The singular name of an entity, used in transaction names; e.g. "artist".
     */
    private val entityName: String,
    private val savedEntityTable: SavedEntityTable,
    private val libraryUpdateKey: String = savedEntityTable.tableName,
) : SavedRepository() {
    private var libraryStateInitialized = false

    private val libraryState: MutableState<Set<String>?> = mutableStateOf(null)

    private val events = MutableSharedFlow<Event>()

    /**
     * Fetches the saved state of each of the given [ids] via a remote call to the network.
     *
     * This is the remote primitive and simply fetches the network state but does not cache it, unlike [getRemote].
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

    override fun eventsFlow(): SharedFlow<Event> = events.asSharedFlow()

    override suspend fun savedTimeCached(id: String): Instant? {
        return KotifyDatabase.transaction("check saved time for $entityName $id") {
            savedEntityTable.savedTime(entityId = id)
        }
    }

    final override suspend fun getCached(ids: Iterable<String>): List<Boolean?> {
        return KotifyDatabase.transaction("check saved state for ${ids.count()} ${entityName}s") {
            val hasFetchedLibrary = GlobalUpdateTimesRepository.hasBeenUpdated(libraryUpdateKey)
            // if we've fetched the entire library, then any saved entity not present in the table is unsaved (or was
            // when the library was fetched)
            val default = if (hasFetchedLibrary) false else null
            ids.map { id -> savedEntityTable.isSaved(entityId = id) ?: default }
        }
            .also { results ->
                val queryEvents = ids.zip(results) { id, result ->
                    QueryEvent(id = id, result = result)
                }
                events.emit(Event.QueryCached(queryEvents))
            }
    }

    final override suspend fun getRemote(id: String): Boolean {
        val saved = fetchIsSaved(ids = listOf(id)).first()

        KotifyDatabase.transaction("set saved state for $entityName $id") {
            savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = null)
        }

        states[id]?.get()?.value = saved
        libraryState.value?.let { library ->
            libraryState.value = library.plusOrMinus(value = id, condition = saved)
        }
        val queryEvents = listOf(QueryEvent(id = id, result = saved))
        events.emit(Event.QueryRemote(queryEvents))

        return saved
    }

    final override suspend fun getRemote(ids: List<String>): List<Boolean> {
        val saveds = fetchIsSaved(ids = ids)

        KotifyDatabase.transaction("set saved state for ${ids.size} ${entityName}s") {
            ids.zipEach(saveds) { id, saved ->
                savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = null)
            }
        }

        ids.zipEach(saveds) { id, saved ->
            states[id]?.get()?.value = saved
            libraryState.value?.let { library ->
                libraryState.value = library.plusOrMinus(value = id, condition = saved)
            }
        }

        val queryEvents = ids.zip(saveds).map { (id, saved) -> QueryEvent(id = id, result = saved) }
        events.emit(Event.QueryRemote(queryEvents))

        return saveds
    }

    final override suspend fun libraryState(fetchIfUnknown: Boolean): State<Set<String>?> {
        // library state has never been loaded, i.e. the database has never been checked; load it from cache now
        if (!libraryStateInitialized) {
            getLibraryCached()
            assert(libraryStateInitialized)
        }

        // if the library state is still missing and fetchIfUnknown is true, fetch from the remote
        if (fetchIfUnknown && libraryState.value == null) {
            getLibraryRemote()
        }

        return libraryState
    }

    /**
     * Clears the cache of states used by [stateOf], for use in tests.
     */
    fun clearStates() {
        states.clear()
    }

    final override suspend fun setSaved(ids: List<String>, saved: Boolean) {
        pushSaved(ids = ids, saved = saved)

        KotifyDatabase.transaction("set saved state for ${ids.size} ${entityName}s") {
            ids.forEach { id -> savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = Instant.now()) }
        }

        ids.forEach { id ->
            states[id]?.get()?.value = saved
        }
        libraryState.value?.let { library ->
            libraryState.value = library.plusOrMinus(elements = ids, condition = saved)
        }
        events.emit(Event.SetSaved(ids = ids, saved = saved))
    }

    final override suspend fun libraryUpdated(): Instant? {
        return KotifyDatabase.transaction("check $entityName saved library updated time") {
            GlobalUpdateTimesRepository.updated(libraryUpdateKey)
        }
    }

    final override suspend fun invalidateLibrary() {
        KotifyDatabase.transaction("invalidate $entityName saved library") {
            GlobalUpdateTimesRepository.invalidate(libraryUpdateKey)
        }

        events.emit(Event.InvalidateLibrary)
    }

    final override suspend fun getLibraryCached(): Set<String>? {
        return KotifyDatabase.transaction("load $entityName saved library") {
            if (GlobalUpdateTimesRepository.hasBeenUpdated(libraryUpdateKey)) {
                savedEntityTable.savedEntityIds()
            } else {
                null
            }
        }
            .also { library ->
                libraryStateInitialized = true
                libraryState.value = library

                events.emit(Event.QueryLibraryCached(library = library))
            }
    }

    final override suspend fun getLibraryRemote(): Set<String> {
        val savedNetworkModels = fetchLibrary()
        return KotifyDatabase.transaction("save $entityName saved library") {
            GlobalUpdateTimesRepository.setUpdated(libraryUpdateKey)
            savedNetworkModels.mapNotNullTo(mutableSetOf()) { from(it) }
                .onEach { id ->
                    savedEntityTable.setSaved(entityId = id, saved = true, savedTime = null)
                }
        }
            .also { library ->
                for ((id, reference) in states.entries) {
                    reference.get()?.value = library.contains(id)
                }

                libraryStateInitialized = true
                libraryState.value = library

                events.emit(Event.QueryLibraryRemote(library = library))
            }
    }

    final override suspend fun invalidateAll() {
        KotifyDatabase.transaction("invalidate $entityName saved library and entities") {
            GlobalUpdateTimesRepository.invalidate(libraryUpdateKey)
            savedEntityTable.deleteAll()
        }
        states.clear()
    }
}
