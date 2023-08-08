package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.repository.global.GlobalUpdateTimesRepository
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.util.CachedResource
import com.dzirbel.kotify.repository.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.repository.util.ToggleableState
import com.dzirbel.kotify.repository.util.midpointInstantToNow
import com.dzirbel.kotify.util.filterNotNullValues
import com.dzirbel.kotify.util.plusOrMinus
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.TimeSource

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
     * The base key used in the [GlobalUpdateTimesRepository] to mark when the library as a whole was last updated;
     * augmented with the current user ID to form the full key [currentUserLibraryUpdateKey].
     */
    private val baseLibraryUpdateKey: String = savedEntityTable.tableName,

    private val scope: CoroutineScope,
) : SavedRepository {
    private val libraryResource: CachedResource<SavedRepository.Library> = CachedResource(
        scope = scope,
        getFromCache = { getLibraryCached() },
        getFromRemote = { getLibraryRemote() },
    )

    override val library: StateFlow<SavedRepository.Library?>
        get() = libraryResource.flow.also { Repository.checkEnabled() }.also { libraryResource.ensureLoaded() }

    override val libraryRefreshing: StateFlow<Boolean>
        get() = libraryResource.refreshingFlow.also { Repository.checkEnabled() }

    private val currentUserLibraryUpdateKey: String
        get() = "$baseLibraryUpdateKey-${UserRepository.requireCurrentUserId}".also { Repository.checkEnabled() }

    private val savedStates = SynchronizedWeakStateFlowMap<String, ToggleableState<Boolean>>()

    /**
     * Fetches the saved state of each of the given [ids] via a remote call to the network.
     *
     * This is the remote primitive and simply fetches the network state but does not cache it.
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
     * This is the remote primitive and simply fetches the network state but does not cache it.
     */
    protected abstract suspend fun fetchLibrary(): Iterable<SavedNetworkType>

    /**
     * Converts the given [savedNetworkType] model into the ID of the saved entity, and adds any corresponding model to
     * the database.
     *
     * E.g. for saved artists, the attached artist model should be inserted into/used to update the database (typically
     * via its repository) and its ID returned. Always called from within a transaction.
     * TODO also update live states when converting
     *
     * @return the extracted entity ID and an [Instant] specifying the time the entity was saved, if provided by the
     *  [SavedNetworkType]
     */
    protected abstract fun convert(savedNetworkType: SavedNetworkType): Pair<String, Instant?>

    final override fun init() {
        Repository.checkEnabled()
        libraryResource.initFromCache()
    }

    final override fun refreshLibrary() {
        Repository.checkEnabled()
        libraryResource.refreshFromRemote()
    }

    final override fun savedStateOf(id: String): StateFlow<ToggleableState<Boolean>?> {
        Repository.checkEnabled()
        return savedStates.getOrCreateStateFlow(
            key = id,
            defaultValue = {
                libraryResource.flow.value?.ids?.contains(id)?.let { ToggleableState.Set(it) }
            },
            onCreate = { default ->
                if (default == null) {
                    val userId = UserRepository.requireCurrentUserId
                    scope.launch {
                        val cached = try {
                            KotifyDatabase.transaction("load save state of $entityName $id") {
                                savedEntityTable.isSaved(entityId = id, userId = userId)
                            }
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (_: Throwable) {
                            // TODO log exception?
                            null
                        }

                        if (cached != null) {
                            savedStates.updateValue(id, ToggleableState.Set(cached))
                        } else {
                            val start = TimeSource.Monotonic.markNow()
                            val remote = try {
                                fetchIsSaved(ids = listOf(id)).first()
                            } catch (cancellationException: CancellationException) {
                                throw cancellationException
                            } catch (_: Throwable) {
                                // TODO log exception?
                                null
                            }

                            // TODO expose error state instead of null?
                            savedStates.updateValue(id, remote?.let { ToggleableState.Set(remote) })

                            if (remote != null) {
                                val saveTime = start.midpointInstantToNow()
                                KotifyDatabase.transaction("set save state of $entityName $id") {
                                    savedEntityTable.setSaved(
                                        entityId = id,
                                        userId = userId,
                                        saved = remote,
                                        savedTime = null,
                                        savedCheckTime = saveTime,
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    final override fun savedStatesOf(ids: Iterable<String>): List<StateFlow<ToggleableState<Boolean>?>> {
        Repository.checkEnabled()
        return savedStates.getOrCreateStateFlows(
            keys = ids,
            defaultValue = { id ->
                libraryResource.flow.value?.ids?.contains(id)?.let { ToggleableState.Set(it) }
            },
            onCreate = { creations ->
                val missingIds = creations.filterNotNullValues().keys
                if (missingIds.isNotEmpty()) {
                    val userId = UserRepository.requireCurrentUserId
                    scope.launch {
                        val cached = try {
                            KotifyDatabase.transaction("load save states of ${missingIds.size} ${entityName}s") {
                                missingIds.map { id ->
                                    savedEntityTable.isSaved(entityId = id, userId = userId)
                                }
                            }
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (_: Throwable) {
                            // TODO log exception?
                            null
                        }

                        val idsToLoadFromRemote = mutableListOf<String>()
                        if (cached == null) {
                            idsToLoadFromRemote.addAll(missingIds)
                        } else {
                            missingIds.zipEach(cached) { id, saved ->
                                if (saved == null) {
                                    idsToLoadFromRemote.add(id)
                                } else {
                                    savedStates.updateValue(id, ToggleableState.Set(saved))
                                }
                            }
                        }

                        if (idsToLoadFromRemote.isNotEmpty()) {
                            val start = TimeSource.Monotonic.markNow()
                            val remote = try {
                                fetchIsSaved(ids = idsToLoadFromRemote)
                            } catch (cancellationException: CancellationException) {
                                throw cancellationException
                            } catch (_: Throwable) {
                                // TODO log exception?
                                null
                            }

                            // TODO expose error state instead of null?
                            if (remote != null) {
                                idsToLoadFromRemote.zipEach(remote) { id, saved ->
                                    savedStates.updateValue(id, ToggleableState.Set(saved))
                                }

                                val saveTime = start.midpointInstantToNow()
                                KotifyDatabase.transaction(
                                    "set save state of ${idsToLoadFromRemote.size} ${entityName}s",
                                ) {
                                    savedEntityTable.setSaved(
                                        entityIds = idsToLoadFromRemote,
                                        saved = remote,
                                        userId = userId,
                                        savedTime = null,
                                        savedCheckTime = saveTime,
                                    )
                                }
                            } else {
                                idsToLoadFromRemote.forEach { id -> savedStates.updateValue(id, null) }
                            }
                        }
                    }
                }
            },
        )
    }

    final override fun setSaved(id: String, saved: Boolean) {
        Repository.checkEnabled()
        val saveTime = Instant.now()

        // TODO prevent concurrent updates to saved state for the same id
        scope.launch {
            savedStates.updateValue(id, ToggleableState.TogglingTo(saved))

            try {
                pushSaved(ids = listOf(id), saved = saved)
            } catch (throwable: Throwable) {
                // TODO log exception?
                savedStates.updateValue(id, null) // TODO expose error state?
                throw throwable
            }

            KotifyDatabase.transaction("set saved state for $entityName $id") {
                savedEntityTable.setSaved(
                    entityId = id,
                    userId = UserRepository.requireCurrentUserId,
                    saved = saved,
                    savedTime = saveTime,
                    savedCheckTime = saveTime,
                )
            }

            ensureActive()

            savedStates.updateValue(id, ToggleableState.Set(saved))

            libraryResource.update { library ->
                library.copy(ids = library.ids.plusOrMinus(value = id, condition = saved))
            }
        }
    }

    final override fun invalidateUser() {
        Repository.checkEnabled()
        libraryResource.invalidate()
        savedStates.clear()
    }

    // TODO catch and log exceptions
    private suspend fun getLibraryCached(): SavedRepository.Library? {
        if (!UserRepository.hasCurrentUserId) return null

        return KotifyDatabase.transaction("load $entityName saved library") {
            GlobalUpdateTimesRepository.updated(currentUserLibraryUpdateKey)?.let { updatedTime ->
                updatedTime to savedEntityTable.savedEntityIds(userId = UserRepository.requireCurrentUserId)
            }
        }
            ?.let { (updatedTime, ids) ->
                savedStates.computeAll { id -> ToggleableState.Set(id in ids) }
                SavedRepository.Library(ids, updatedTime)
            }
    }

    // TODO catch and log exceptions
    private suspend fun getLibraryRemote(): SavedRepository.Library {
        val userId = UserRepository.requireCurrentUserId

        val start = TimeSource.Monotonic.markNow()
        val savedNetworkModels = fetchLibrary()
        val updateTime = start.midpointInstantToNow()

        val remoteLibrary = KotifyDatabase.transaction("save $entityName saved library") {
            GlobalUpdateTimesRepository.setUpdated(key = currentUserLibraryUpdateKey, updateTime = updateTime)

            val cachedLibrary: Set<String> = libraryResource.flow.value?.ids
                ?: savedEntityTable.savedEntityIds(userId = userId)

            val remoteLibrary: Set<Pair<String, Instant?>> = savedNetworkModels.mapTo(mutableSetOf(), ::convert)
            val remoteLibraryIds: Set<String> = remoteLibrary.mapTo(mutableSetOf()) { it.first }

            // remove saved records for entities which are no longer saved
            for (id in cachedLibrary) {
                if (id !in remoteLibraryIds) {
                    savedEntityTable.setSaved(
                        entityId = id,
                        userId = userId,
                        saved = false,
                        savedTime = null,
                        savedCheckTime = updateTime,
                    )
                }
            }

            // add saved records for entities which are now saved
            for ((id, saveTime) in remoteLibrary) {
                if (id !in cachedLibrary) {
                    savedEntityTable.setSaved(
                        entityId = id,
                        userId = userId,
                        saved = true,
                        savedTime = saveTime,
                        savedCheckTime = updateTime,
                    )
                }
            }

            remoteLibraryIds
        }

        savedStates.computeAll { id -> ToggleableState.Set(id in remoteLibrary) }
        return SavedRepository.Library(remoteLibrary, updateTime)
    }
}
