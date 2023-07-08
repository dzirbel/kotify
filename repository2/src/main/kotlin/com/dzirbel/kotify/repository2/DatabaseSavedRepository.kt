package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.repository.global.GlobalUpdateTimesRepository
import com.dzirbel.kotify.repository.player.JobLock
import com.dzirbel.kotify.repository.player.ToggleableState
import com.dzirbel.kotify.repository2.util.SynchronizedWeakStateFlowMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

// TODO invalidate on sign-out
abstract class DatabaseSavedRepository<SavedNetworkType>(
    /**
     * The singular name of an entity, used in transaction names; e.g. "artist".
     */
    private val savedEntityTable: SavedEntityTable,
    private val entityName: String = savedEntityTable.tableName.removePrefix("saved_").removeSuffix("s"),
    private val libraryUpdateKey: String = savedEntityTable.tableName,
) : SavedRepository {
    private val _library = MutableStateFlow<CacheState<Set<String>>?>(null)
    override val library: StateFlow<CacheState<Set<String>>?>
        get() = _library

    private val refreshLibraryLock = JobLock()

    private val savedStates = SynchronizedWeakStateFlowMap<String, ToggleableState<Boolean>>()

    // TODO do not run in tests?
    init {
        Repository.scope.launch {
            getLibraryCached()
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

    override fun savedStateOf(id: String): StateFlow<ToggleableState<Boolean>?> {
        return savedStates.getOrCreateStateFlow(id)
    }

    // TODO error handling
    override fun ensureSavedStateLoaded(id: String) {
        // TODO no-op if the entire library has been loaded?
        Repository.scope.launch {
            if (savedStates.getValue(id) == null) {
                val cached = savedEntityTable.isSaved(id)
                if (cached != null) {
                    savedStates.updateValue(id, ToggleableState.Set(cached))
                } else {
                    // TODO set state to refreshing? or just use the null value
                    val remote = fetchIsSaved(ids = listOf(id)).first()
                    savedStates.updateValue(id, ToggleableState.Set(remote))
                }
            }
        }
    }

    override fun refreshLibrary() {
        refreshLibraryLock.launch(Repository.scope) {
            _library.value = CacheState.Refreshing(
                cachedValue = _library.value?.cachedValue,
                cacheTime = _library.value?.cacheTime,
            )
            getLibraryRemote()
        }
    }

    override fun setSaved(id: String, saved: Boolean) {
        // TODO synchronization
        Repository.scope.launch {
            savedStates.updateValue(id, ToggleableState.TogglingTo(saved))

            pushSaved(ids = listOf(id), saved = saved)

            savedStates.updateValue(id, ToggleableState.Set(saved))

            // TODO verify?
        }
    }

    private suspend fun getLibraryCached(): Set<String>? {
        var updatedTime: Instant? = null
        return KotifyDatabase.transaction("load $entityName saved library") {
            updatedTime = GlobalUpdateTimesRepository.updated(libraryUpdateKey)
            updatedTime?.let { savedEntityTable.savedEntityIds() }
        }
            ?.also { ids ->
                savedStates.computeAll { id -> ToggleableState.Set(id in ids) }

                updatedTime?.let {
                    _library.value = CacheState.Loaded(cachedValue = ids, cacheTime = it)
                }
            }
    }

    private suspend fun getLibraryRemote(): Set<String> {
        val savedNetworkModels = fetchLibrary()
        val updateTime = Instant.now()
        return KotifyDatabase.transaction("save $entityName saved library") {
            GlobalUpdateTimesRepository.setUpdated(libraryUpdateKey, updateTime = updateTime)

            // TODO use existing cache in library flow if available?
            val cachedLibrary = savedEntityTable.savedEntityIds()
            val remoteLibrary = savedNetworkModels.mapNotNullTo(mutableSetOf()) { from(it) }

            // remove saved records for entities which are no longer saved
            val newSaves = cachedLibrary.minus(remoteLibrary)
            if (newSaves.isNotEmpty()) {
                savedEntityTable.setSaved(entityIds = newSaves, saved = true, savedCheckTime = updateTime)
            }

            // add saved records for entities which are now saved
            val removedSaves = remoteLibrary.minus(cachedLibrary)
            if (removedSaves.isNotEmpty()) {
                savedEntityTable.setSaved(entityIds = removedSaves, saved = true, savedCheckTime = updateTime)
            }

            remoteLibrary
        }
            .also { ids ->
                savedStates.computeAll { id -> ToggleableState.Set(id in ids) }

                _library.value = CacheState.Loaded(cachedValue = ids, cacheTime = updateTime)

                savedStates
            }
    }
}
