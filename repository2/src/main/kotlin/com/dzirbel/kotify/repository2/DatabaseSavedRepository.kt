package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.repository.global.GlobalUpdateTimesRepository
import com.dzirbel.kotify.repository2.user.UserRepository
import com.dzirbel.kotify.repository2.util.JobLock
import com.dzirbel.kotify.repository2.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.repository2.util.ToggleableState
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant

abstract class DatabaseSavedRepository<SavedNetworkType>(
    /**
     * The [SavedEntityTable] holding database entries of saved states.
     */
    private val savedEntityTable: SavedEntityTable,

    /**
     * The singular name of an entity, used in transaction names; e.g. "artist".
     */
    private val entityName: String = savedEntityTable.tableName.removePrefix("saved_").removeSuffix("s"),

    /**
     * The key used in the [GlobalUpdateTimesRepository] to mark when the library as a whole was last updated.
     */
    private val baseLibraryUpdateKey: String = savedEntityTable.tableName,
) : SavedRepository {
    private val _library = MutableStateFlow<CacheState<Set<String>>?>(null)
    override val library: StateFlow<CacheState<Set<String>>?>
        get() = _library

    private val refreshLibraryLock = JobLock()

    private val currentUserLibraryUpdateKey: String
        get() = "$baseLibraryUpdateKey-${UserRepository.requireCurrentUserId}"

    private val savedStates = SynchronizedWeakStateFlowMap<String, ToggleableState<Boolean>>()

    override fun init() {
        refreshLibraryLock.launch(Repository.scope) {
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
    protected abstract fun from(savedNetworkType: SavedNetworkType): String

    override fun savedStateOf(id: String): StateFlow<ToggleableState<Boolean>?> {
        return savedStates.getOrCreateStateFlow(id) {
            ensureSavedStateLoaded(id)
        }
    }

    override fun savedStatesOf(ids: Iterable<String>): List<StateFlow<ToggleableState<Boolean>?>> {
        return savedStates.getOrCreateStateFlows(ids) { createdIds ->
            Repository.scope.launch {
                val cached = KotifyDatabase.transaction("load save states of ${createdIds.size} ${entityName}s") {
                    createdIds.map { id ->
                        savedEntityTable.isSaved(entityId = id, userId = UserRepository.requireCurrentUserId)
                    }
                }

                val missingIds = mutableListOf<String>()
                ids.zipEach(cached) { id, saved ->
                    if (saved == null) {
                        missingIds.add(id)
                    } else {
                        savedStates.updateValue(id, ToggleableState.Set(saved))
                    }
                }

                if (missingIds.isNotEmpty()) {
                    val remote = fetchIsSaved(ids = missingIds)
                    missingIds.zipEach(remote) { id, saved ->
                        savedStates.updateValue(id, ToggleableState.Set(saved))
                    }
                }
            }
        }
    }

    // TODO error handling
    private fun ensureSavedStateLoaded(id: String) {
        // TODO no-op if the entire library has been loaded?
        Repository.scope.launch {
            if (savedStates.getValue(id) == null) {
                val cached = KotifyDatabase.transaction("load save state of $id") {
                    savedEntityTable.isSaved(entityId = id, userId = UserRepository.requireCurrentUserId)
                }
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

            // TODO update state in database

            savedStates.updateValue(id, ToggleableState.Set(saved))

            // TODO verify?
        }
    }

    override fun invalidateUser() {
        _library.value = null
        savedStates.clear()
        // TODO cancel any refreshes on refreshLibraryLock?
    }

    private suspend fun getLibraryCached(): Set<String>? {
        return KotifyDatabase.transaction("load $entityName saved library") {
            GlobalUpdateTimesRepository.updated(currentUserLibraryUpdateKey)?.let { updatedTime ->
                updatedTime to savedEntityTable.savedEntityIds(userId = UserRepository.requireCurrentUserId)
            }
        }
            ?.let { (updatedTime, ids) ->
                savedStates.computeAll { id -> ToggleableState.Set(id in ids) }
                _library.value = CacheState.Loaded(cachedValue = ids, cacheTime = updatedTime)
                ids
            }
    }

    private suspend fun getLibraryRemote(): Set<String> {
        val savedNetworkModels = fetchLibrary()
        val updateTime = Instant.now()
        val userId = UserRepository.requireCurrentUserId
        return KotifyDatabase.transaction("save $entityName saved library") {
            GlobalUpdateTimesRepository.setUpdated(currentUserLibraryUpdateKey, updateTime = updateTime)

            // TODO use existing cache in library flow if available?
            val cachedLibrary: Set<String> = savedEntityTable.savedEntityIds(userId = userId)
            val remoteLibrary: Set<String> = savedNetworkModels.mapTo(mutableSetOf()) { from(it) }

            // remove saved records for entities which are no longer saved
            val removedSaves = cachedLibrary.minus(remoteLibrary)
            if (removedSaves.isNotEmpty()) {
                savedEntityTable.setSaved(
                    entityIds = removedSaves,
                    userId = userId,
                    saved = false,
                    savedTime = null,
                    savedCheckTime = updateTime,
                )
            }

            // add saved records for entities which are now saved
            val newSaves = remoteLibrary.minus(cachedLibrary)
            if (newSaves.isNotEmpty()) {
                savedEntityTable.setSaved(
                    entityIds = newSaves,
                    userId = userId,
                    saved = true,
                    savedTime = null,
                    savedCheckTime = updateTime,
                )
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
