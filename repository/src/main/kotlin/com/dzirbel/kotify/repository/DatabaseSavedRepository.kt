package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.DB
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.asLog
import com.dzirbel.kotify.repository.global.GlobalUpdateTimesRepository
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.util.CachedResource
import com.dzirbel.kotify.repository.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.repository.util.ToggleableState
import com.dzirbel.kotify.repository.util.midpointInstantToNow
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.collections.plusOrMinus
import com.dzirbel.kotify.util.collections.zipEach
import kotlinx.coroutines.CoroutineScope
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
        getFromCache = ::getLibraryCached,
        getFromRemote = ::getLibraryRemote,
    )

    override val library: StateFlow<SavedRepository.Library?>
        get() = libraryResource.flow.also { Repository.checkEnabled() }.also { libraryResource.ensureLoaded() }

    override val libraryRefreshing: StateFlow<Boolean>
        get() = libraryResource.refreshingFlow.also { Repository.checkEnabled() }

    private val currentUserLibraryUpdateKey: String
        get() = "$baseLibraryUpdateKey-${UserRepository.requireCurrentUserId}".also { Repository.checkEnabled() }

    private val savedStates = SynchronizedWeakStateFlowMap<String, ToggleableState<Boolean>>()

    private val mutableLog = MutableLog<Repository.LogData>(
        name = requireNotNull(this::class.qualifiedName).removeSuffix(".Companion").substringAfterLast('.'),
        scope = scope,
    )

    override val log = mutableLog.asLog()

    /**
     * Fetches the saved state of each of the given [ids] via a remote call to the network.
     *
     * This is the remote primitive and simply fetches the network state but does not cache it.
     */
    protected abstract suspend fun fetchIsSaved(ids: List<String>): List<Boolean>

    /**
     * Fetches the saved state of each of the given [id] via a remote call to the network.
     *
     * This is the remote primitive and simply fetches the network state but does not cache it.
     */
    protected open suspend fun fetchIsSaved(id: String): Boolean = fetchIsSaved(listOf(id)).first()

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
    protected abstract fun convertToDB(savedNetworkType: SavedNetworkType, fetchTime: Instant): Pair<String, Instant?>

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

        val requestLog = RequestLog(log = mutableLog)
        return savedStates.getOrCreateStateFlow(
            key = id,
            defaultValue = {
                libraryResource.flow.value?.ids?.contains(id)?.let { ToggleableState.Set(it) }
            },
            onExisting = {
                requestLog.info("save state for $entityName $id in memory", DataSource.MEMORY)
            },
            onCreate = { default ->
                if (default == null) {
                    val userId = UserRepository.requireCurrentUserId
                    scope.launch {
                        val dbStart = TimeSource.Monotonic.markNow()
                        val cached: Boolean? = try {
                            KotifyDatabase[DB.CACHE].transaction("load save state of $entityName $id") {
                                savedEntityTable.isSaved(entityId = id, userId = userId)
                            }
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (throwable: Throwable) {
                            requestLog
                                .addDbTime(dbStart.elapsedNow())
                                .error(
                                    throwable = throwable,
                                    title = "error loading save state of $entityName $id from database",
                                    source = DataSource.DATABASE,
                                )
                            null
                        }

                        requestLog.addDbTime(dbStart.elapsedNow())

                        if (cached != null) {
                            savedStates.updateValue(id, ToggleableState.Set(cached))
                            requestLog.success(
                                title = "loaded save state of $entityName $id from database",
                                source = DataSource.DATABASE,
                            )
                        } else {
                            val remoteStart = TimeSource.Monotonic.markNow()
                            val remote: Boolean? = try {
                                fetchIsSaved(id = id)
                            } catch (cancellationException: CancellationException) {
                                throw cancellationException
                            } catch (throwable: Throwable) {
                                requestLog
                                    .addRemoteTime(remoteStart.elapsedNow())
                                    .error(
                                        throwable = throwable,
                                        title = "error loading save state of $entityName $id from remote",
                                        source = DataSource.REMOTE,
                                    )
                                null
                            }

                            requestLog.addRemoteTime(remoteStart.elapsedNow())

                            if (remote == null) {
                                // TODO expose error state instead of null?
                                savedStates.updateValue(id, null)
                                requestLog.warn(
                                    title = "save state of $entityName $id not found from remote",
                                    source = DataSource.REMOTE,
                                )
                            } else {
                                val saveTime = remoteStart.midpointInstantToNow()

                                savedStates.updateValue(id, ToggleableState.Set(remote))
                                requestLog.success(
                                    title = "loaded save state of $entityName $id from remote",
                                    source = DataSource.REMOTE,
                                )

                                try {
                                    KotifyDatabase[DB.CACHE].transaction("set save state of $entityName $id") {
                                        savedEntityTable.setSaved(
                                            entityId = id,
                                            userId = userId,
                                            saved = remote,
                                            savedTime = null,
                                            savedCheckTime = saveTime,
                                        )
                                    }
                                } catch (cancellationException: CancellationException) {
                                    throw cancellationException
                                } catch (throwable: Throwable) {
                                    // TODO use manual time in DB?
                                    requestLog.warn(
                                        throwable = throwable,
                                        title = "error saving remote save state of $entityName $id in database",
                                        source = DataSource.DATABASE,
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

        val requestLog = RequestLog(log = mutableLog)
        return savedStates.getOrCreateStateFlows(
            keys = ids,
            defaultValue = { id ->
                libraryResource.flow.value?.ids?.contains(id)?.let { ToggleableState.Set(it) }
            },
            onExisting = { numExisting ->
                requestLog.info("$numExisting/${ids.count()} $entityName save states in memory", DataSource.MEMORY)
            },
            onCreate = { creations ->
                // only load state if it could not be initialized from the library
                val missingIds = creations.mapNotNull { if (it.value == null) it.key else null }
                if (missingIds.isNotEmpty()) {
                    val userId = UserRepository.requireCurrentUserId
                    scope.launch {
                        val dbStart = TimeSource.Monotonic.markNow()
                        val cached = try {
                            KotifyDatabase[DB.CACHE].transaction(
                                name = "load save states of ${missingIds.size} ${entityName}s",
                            ) {
                                missingIds.map { id ->
                                    savedEntityTable.isSaved(entityId = id, userId = userId)
                                }
                            }
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (throwable: Throwable) {
                            requestLog
                                .addDbTime(dbStart.elapsedNow())
                                .error(
                                    throwable = throwable,
                                    title = "error loading save states of ${missingIds.size} ${entityName}s",
                                    source = DataSource.DATABASE,
                                )
                            null
                        }

                        requestLog.addDbTime(dbStart.elapsedNow())

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

                        val numFromDb = missingIds.size - idsToLoadFromRemote.size
                        if (numFromDb > 0) {
                            requestLog.success(
                                title = "loaded $numFromDb/${missingIds.size} $entityName save states from database",
                                source = DataSource.DATABASE,
                            )
                        }

                        if (idsToLoadFromRemote.isNotEmpty()) {
                            val remoteStart = TimeSource.Monotonic.markNow()
                            val remote = try {
                                fetchIsSaved(ids = idsToLoadFromRemote)
                            } catch (cancellationException: CancellationException) {
                                throw cancellationException
                            } catch (throwable: Throwable) {
                                requestLog
                                    .addRemoteTime(remoteStart.elapsedNow())
                                    .error(
                                        throwable = throwable,
                                        title = "error loading save states of " +
                                            "${idsToLoadFromRemote.size} ${entityName}s from remote",
                                        source = DataSource.REMOTE,
                                    )
                                null
                            }

                            requestLog.addRemoteTime(remoteStart.elapsedNow())

                            if (remote == null) {
                                // TODO expose error state instead of null?
                                idsToLoadFromRemote.forEach { id -> savedStates.updateValue(id, null) }
                                requestLog.warn(
                                    title = "save states of ${idsToLoadFromRemote.size} ${entityName}s not found " +
                                        "from remote",
                                    source = DataSource.REMOTE,
                                )
                            } else {
                                val saveTime = remoteStart.midpointInstantToNow()

                                idsToLoadFromRemote.zipEach(remote) { id, saved ->
                                    savedStates.updateValue(id, ToggleableState.Set(saved))
                                }

                                requestLog.success(
                                    title = "loaded save states of ${idsToLoadFromRemote.size} ${entityName}s " +
                                        "from remote",
                                    source = DataSource.REMOTE,
                                )

                                try {
                                    KotifyDatabase[DB.CACHE].transaction(
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
                                } catch (cancellationException: CancellationException) {
                                    throw cancellationException
                                } catch (throwable: Throwable) {
                                    // TODO use manual time in DB?
                                    requestLog.warn(
                                        throwable = throwable,
                                        title = "error saving remote save states of ${idsToLoadFromRemote.size} " +
                                            "${entityName}s in database",
                                        source = DataSource.DATABASE,
                                    )
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    final override fun setSaved(id: String, saved: Boolean) {
        Repository.checkEnabled()
        val saveTime = CurrentTime.instant

        // TODO prevent concurrent updates to saved state for the same id
        val requestLog = RequestLog(log = mutableLog)
        scope.launch {
            savedStates.updateValue(id, ToggleableState.TogglingTo(saved))

            val remoteStart = TimeSource.Monotonic.markNow()
            try {
                pushSaved(ids = listOf(id), saved = saved)
            } catch (throwable: Throwable) {
                savedStates.updateValue(id, null) // TODO expose error state?
                requestLog
                    .addRemoteTime(remoteStart.elapsedNow())
                    .error(
                        title = "error setting saved state of $entityName $id to $saved in remote",
                        source = DataSource.REMOTE,
                        throwable = throwable,
                    )
                throw throwable
            }

            requestLog.addRemoteTime(remoteStart.elapsedNow())

            val dbStart = TimeSource.Monotonic.markNow()
            try {
                KotifyDatabase[DB.CACHE].transaction("set saved state for $entityName $id") {
                    savedEntityTable.setSaved(
                        entityId = id,
                        userId = UserRepository.requireCurrentUserId,
                        saved = saved,
                        savedTime = saveTime,
                        savedCheckTime = saveTime,
                    )
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                requestLog
                    .addDbTime(dbStart.elapsedNow())
                    .error(
                        title = "error saving saved state of $entityName $id to $saved in database",
                        source = DataSource.DATABASE,
                        throwable = throwable,
                    )
                @Suppress("LabeledExpression")
                return@launch
            }

            requestLog.addDbTime(dbStart.elapsedNow())

            savedStates.updateValue(id, ToggleableState.Set(saved))

            libraryResource.update { library ->
                library.copy(ids = library.ids.plusOrMinus(value = id, condition = saved))
            }

            requestLog.success("set saved state of $entityName $id to $saved", DataSource.REMOTE)
        }
    }

    final override fun invalidateUser() {
        Repository.checkEnabled()
        libraryResource.invalidate()
        savedStates.clear()
    }

    private suspend fun getLibraryCached(): SavedRepository.Library? {
        if (!UserRepository.hasCurrentUserId) return null

        val requestLog = RequestLog(log = mutableLog)
        val dbStart = TimeSource.Monotonic.markNow()
        return try {
            KotifyDatabase[DB.CACHE].transaction("load $entityName saved library") {
                GlobalUpdateTimesRepository.updated(currentUserLibraryUpdateKey)?.let { updatedTime ->
                    updatedTime to savedEntityTable.savedEntityIds(userId = UserRepository.requireCurrentUserId)
                }
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            requestLog
                .addDbTime(dbStart.elapsedNow())
                .error("error loading $entityName saved library from database", DataSource.DATABASE, throwable)
            return null
        }
            ?.let { (updatedTime, ids) ->
                requestLog.addDbTime(dbStart.elapsedNow())
                savedStates.computeAll { id -> ToggleableState.Set(id in ids) }
                requestLog.success("loaded $entityName saved library from database", DataSource.DATABASE)
                SavedRepository.Library(ids, updatedTime)
            }
    }

    private suspend fun getLibraryRemote(): SavedRepository.Library {
        val requestLog = RequestLog(log = mutableLog)
        val userId = UserRepository.requireCurrentUserId

        val remoteStart = TimeSource.Monotonic.markNow()

        val savedNetworkModels = try {
            fetchLibrary()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            requestLog
                .addRemoteTime(remoteStart.elapsedNow())
                .error("error loading $entityName saved library from remote", DataSource.REMOTE, throwable)
            throw throwable
        }

        requestLog.addRemoteTime(remoteStart.elapsedNow())
        val fetchTime = remoteStart.midpointInstantToNow()

        val dbStart = TimeSource.Monotonic.markNow()
        val remoteLibrary = try {
            KotifyDatabase[DB.CACHE].transaction("save $entityName saved library") {
                GlobalUpdateTimesRepository.setUpdated(key = currentUserLibraryUpdateKey, updateTime = fetchTime)

                val cachedLibrary: Set<String> = libraryResource.flow.value?.ids
                    ?: savedEntityTable.savedEntityIds(userId = userId)

                val remoteLibrary: List<Pair<String, Instant?>> = savedNetworkModels.map { convertToDB(it, fetchTime) }
                val remoteLibraryIds: Set<String> = remoteLibrary.mapTo(mutableSetOf()) { it.first }

                // remove saved records for entities which are no longer saved
                for (id in cachedLibrary) {
                    if (id !in remoteLibraryIds) {
                        savedEntityTable.setSaved(
                            entityId = id,
                            userId = userId,
                            saved = false,
                            savedTime = null,
                            savedCheckTime = fetchTime,
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
                            savedCheckTime = fetchTime,
                        )
                    }
                }

                remoteLibraryIds
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            requestLog
                .addDbTime(dbStart.elapsedNow())
                .error(
                    title = "error updating database for $entityName saved library from remote",
                    source = DataSource.DATABASE,
                    throwable = throwable,
                )
            throw throwable
        }

        requestLog.addDbTime(dbStart.elapsedNow())

        savedStates.computeAll { id -> ToggleableState.Set(id in remoteLibrary) }

        requestLog.success("loaded $entityName saved library from remote", DataSource.REMOTE)

        return SavedRepository.Library(remoteLibrary, fetchTime)
    }
}
