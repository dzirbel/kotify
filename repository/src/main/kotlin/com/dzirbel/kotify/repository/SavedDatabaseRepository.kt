package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.repository.global.GlobalUpdateTimesRepository
import com.dzirbel.kotify.util.plusOrMinus
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.deleteAll
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

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
    private val libraryStateInitialized = AtomicBoolean(false)

    private val libraryFlow: MutableStateFlow<Set<String>?> = MutableStateFlow(null)
    private val libraryUpdatedFlow: MutableStateFlow<Instant?> = MutableStateFlow(null)

    private val events = MutableSharedFlow<Event>()

    /**
     * Atomically initializes the values of [libraryState] and [libraryUpdatedFlow], and must be invoked before the
     * values of these flows are used.
     *
     * This function is idempotent unless [allowRemote] is true, in which case a new asynchronous call to
     * [getLibraryRemote] will be made if the current state of the library is unknown.
     *
     * In effect, this asynchronously checks the value of the library from the database and if [allowRemote] is true and
     * there is no cached library then also fetches it from the remote source.
     */
    private fun initFlows(allowCache: Boolean, allowRemote: Boolean, scope: CoroutineScope) {
        if (!libraryStateInitialized.getAndSet(true)) {
            scope.launch {
                val cached = if (allowCache) getLibraryCached() else null
                if (allowRemote && cached == null) {
                    getLibraryRemote()
                }
            }
        } else if (allowRemote && libraryFlow.value == null) {
            scope.launch {
                getLibraryRemote()
            }
        }
    }

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

        updateLiveState(id = id, value = saved)
        libraryFlow.value?.let { library ->
            libraryFlow.value = library.plusOrMinus(value = id, condition = saved)
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
            updateLiveState(id = id, value = saved)
            libraryFlow.value?.let { library ->
                libraryFlow.value = library.plusOrMinus(value = id, condition = saved)
            }
        }

        val queryEvents = ids.zip(saveds).map { (id, saved) -> QueryEvent(id = id, result = saved) }
        events.emit(Event.QueryRemote(queryEvents))

        return saveds
    }

    final override fun libraryState(
        allowCache: Boolean,
        allowRemote: Boolean,
        scope: CoroutineScope,
    ): StateFlow<Set<String>?> {
        initFlows(allowCache = allowCache, allowRemote = allowRemote, scope = scope)
        return libraryFlow
    }

    final override suspend fun setSaved(ids: List<String>, saved: Boolean) {
        pushSaved(ids = ids, saved = saved)

        KotifyDatabase.transaction("set saved state for ${ids.size} ${entityName}s") {
            ids.forEach { id -> savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = Instant.now()) }
        }

        ids.forEach { id ->
            updateLiveState(id = id, value = saved)
        }
        libraryFlow.value?.let { library ->
            libraryFlow.value = library.plusOrMinus(elements = ids, condition = saved)
        }
        events.emit(Event.SetSaved(ids = ids, saved = saved))
    }

    final override suspend fun libraryUpdated(): Instant? {
        return KotifyDatabase.transaction("check $entityName saved library updated time") {
            GlobalUpdateTimesRepository.updated(libraryUpdateKey)
        }
            .also { libraryUpdatedFlow.value = it }
    }

    final override fun libraryUpdatedFlow(scope: CoroutineScope): StateFlow<Instant?> {
        initFlows(allowCache = true, allowRemote = false, scope = scope)
        return libraryUpdatedFlow
    }

    final override suspend fun invalidateLibrary() {
        KotifyDatabase.transaction("invalidate $entityName saved library") {
            GlobalUpdateTimesRepository.invalidate(libraryUpdateKey)
        }
        libraryUpdatedFlow.value = null

        events.emit(Event.InvalidateLibrary)
    }

    final override suspend fun getLibraryCached(): Set<String>? {
        var updatedTime: Instant? = null
        return KotifyDatabase.transaction("load $entityName saved library") {
            updatedTime = GlobalUpdateTimesRepository.updated(libraryUpdateKey)
            updatedTime?.let { savedEntityTable.savedEntityIds() }
        }
            .also { library ->
                libraryStateInitialized.set(true)
                libraryFlow.value = library
                assert(libraryFlow.value == library)
                libraryUpdatedFlow.value = updatedTime

                events.emit(Event.QueryLibraryCached(library = library))
            }
    }

    final override suspend fun getLibraryRemote(): Set<String> {
        val savedNetworkModels = fetchLibrary()
        val updateTime = Instant.now()
        return KotifyDatabase.transaction("save $entityName saved library") {
            GlobalUpdateTimesRepository.setUpdated(libraryUpdateKey, updateTime = updateTime)

            val cachedLibrary = savedEntityTable.savedEntityIds()
            val remoteLibrary = savedNetworkModels.mapNotNullTo(mutableSetOf()) { from(it) }

            // remove saved records for entities which are no longer saved
            cachedLibrary.minus(remoteLibrary).forEach { id ->
                savedEntityTable.setSaved(entityId = id, savedTime = null, saved = false)
            }

            // add saved records for entities which are now saved
            remoteLibrary.minus(cachedLibrary).forEach { id ->
                savedEntityTable.setSaved(entityId = id, savedTime = null, saved = true)
            }

            remoteLibrary
        }
            .also { library ->
                updateLiveStates { id -> library.contains(id) }

                libraryStateInitialized.set(true)
                libraryFlow.value = library
                libraryUpdatedFlow.value = updateTime

                events.emit(Event.QueryLibraryRemote(library = library))
            }
    }

    final override suspend fun invalidateAll() {
        KotifyDatabase.transaction("invalidate $entityName saved library and entities") {
            GlobalUpdateTimesRepository.invalidate(libraryUpdateKey)
            savedEntityTable.deleteAll()
        }
        libraryFlow.value = null
        libraryUpdatedFlow.value = null
        clearStates()
    }

    override fun clearStates() {
        super.clearStates()
        libraryFlow.value = null
        libraryUpdatedFlow.value = null
        libraryStateInitialized.set(false)
    }
}
