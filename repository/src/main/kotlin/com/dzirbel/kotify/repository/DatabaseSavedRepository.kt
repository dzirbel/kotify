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
import com.dzirbel.kotify.repository.util.midpointInstantToNow
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.collections.plusOrMinus
import com.dzirbel.kotify.util.collections.zipEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

abstract class DatabaseSavedRepository<SavedNetworkType>(
    /**
     * The [SavedEntityTable] holding database entries of saved states.
     */
    private val savedEntityTable: SavedEntityTable,

    /**
     * The singular name of an entity, used in transaction names; e.g. "artist".
     */
    override val entityName: String = savedEntityTable.tableName.removePrefix("saved_").removeSuffix("s"),

    /**
     * The base key used in the [GlobalUpdateTimesRepository] to mark when the library as a whole was last updated;
     * augmented with the current user ID to form the full key [currentUserLibraryUpdateKey].
     */
    private val baseLibraryUpdateKey: String = savedEntityTable.tableName,

    private val scope: CoroutineScope,

    protected val userRepository: UserRepository,
) : SavedRepository {
    // TODO add library TTL
    // TODO expose errors via CacheState of library
    private val libraryResource: CachedResource<SavedRepository.Library> = CachedResource(
        scope = scope,
        getFromCache = ::getLibraryCached,
        getFromRemote = ::getLibraryRemote,
    )

    override val library: StateFlow<SavedRepository.Library?>
        get() = libraryResource.flow.also { libraryResource.ensureLoaded() }

    override val libraryRefreshing: StateFlow<Boolean>
        get() = libraryResource.refreshingFlow

    private val currentUserLibraryUpdateKey: String
        get() = "$baseLibraryUpdateKey-${userRepository.requireCurrentUserId}"

    private val savedStates = SynchronizedWeakStateFlowMap<String, SavedRepository.SaveState>()

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
     * via its [ConvertingRepository], with [ConvertingRepository.convertToDB] and [ConvertingRepository.update]) and
     * its ID returned. Always called from within a transaction.
     *
     * @return the extracted entity ID and an [Instant] specifying the time the entity was saved, if provided by the
     *  [SavedNetworkType]
     */
    protected abstract fun convertToDB(savedNetworkType: SavedNetworkType, fetchTime: Instant): Pair<String, Instant?>

    final override fun init() {
        libraryResource.initFromCache()
    }

    final override fun refreshLibrary() {
        libraryResource.refreshFromRemote()
    }

    final override fun savedStateOf(id: String): StateFlow<SavedRepository.SaveState?> {
        val requestLog = RequestLog(log = mutableLog)
        return savedStates.getOrCreateStateFlow(
            key = id,
            defaultValue = {
                libraryResource.flow.value?.ids?.contains(id)?.let { SavedRepository.SaveState.Set(it) }
            },
            onExisting = {
                requestLog.info("save state for $entityName $id in memory", DataSource.MEMORY)
            },
            onCreate = { default ->
                if (default == null) {
                    val userId = userRepository.requireCurrentUserId
                    scope.launch {
                        val dbStart = CurrentTime.mark
                        val cached: SavedEntityTable.SaveState? = try {
                            KotifyDatabase[DB.CACHE].transaction("load save state of $entityName $id") {
                                savedEntityTable.saveState(entityId = id, userId = userId)
                            }
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (throwable: Throwable) {
                            savedStates.updateValue(id, SavedRepository.SaveState.Error(throwable))
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
                            savedStates.updateValue(
                                key = id,
                                value = SavedRepository.SaveState.Set(
                                    saved = cached.saved,
                                    saveTime = cached.savedTime,
                                ),
                            )
                            requestLog.success(
                                title = "loaded save state of $entityName $id from database",
                                source = DataSource.DATABASE,
                            )
                        } else {
                            val remoteStart = CurrentTime.mark
                            val remote: Boolean? = try {
                                fetchIsSaved(id = id)
                            } catch (cancellationException: CancellationException) {
                                throw cancellationException
                            } catch (throwable: Throwable) {
                                savedStates.updateValue(id, SavedRepository.SaveState.Error(throwable))
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
                                savedStates.updateValue(id, SavedRepository.SaveState.NotFound)
                                requestLog.warn(
                                    title = "save state of $entityName $id not found from remote",
                                    source = DataSource.REMOTE,
                                )
                            } else {
                                val saveCheckTime = remoteStart.midpointInstantToNow()

                                savedStates.updateValue(id, SavedRepository.SaveState.Set(saved = remote))
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
                                            savedCheckTime = saveCheckTime,
                                        )
                                    }
                                } catch (cancellationException: CancellationException) {
                                    throw cancellationException
                                } catch (throwable: Throwable) {
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

    final override fun savedStatesOf(ids: Iterable<String>): List<StateFlow<SavedRepository.SaveState?>> {
        val requestLog = RequestLog(log = mutableLog)
        return savedStates.getOrCreateStateFlows(
            keys = ids,
            defaultValue = { id ->
                libraryResource.flow.value?.ids?.contains(id)?.let { SavedRepository.SaveState.Set(it) }
            },
            onExisting = { numExisting ->
                requestLog.info("$numExisting/${ids.count()} $entityName save states in memory", DataSource.MEMORY)
            },
            onCreate = { creations ->
                // only load state if it could not be initialized from the library
                val missingIds = creations.mapNotNull { if (it.value == null) it.key else null }
                if (missingIds.isNotEmpty()) {
                    val userId = userRepository.requireCurrentUserId
                    scope.launch {
                        val dbStart = CurrentTime.mark
                        val cached = try {
                            KotifyDatabase[DB.CACHE].transaction(
                                name = "load save states of ${missingIds.size} ${entityName}s",
                            ) {
                                missingIds.map { id ->
                                    savedEntityTable.saveState(entityId = id, userId = userId)
                                }
                            }
                        } catch (cancellationException: CancellationException) {
                            throw cancellationException
                        } catch (throwable: Throwable) {
                            for (id in missingIds) {
                                savedStates.updateValue(id, SavedRepository.SaveState.Error(throwable))
                            }

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
                            missingIds.zipEach(cached) { id, saveState ->
                                if (saveState == null) {
                                    idsToLoadFromRemote.add(id)
                                } else {
                                    savedStates.updateValue(
                                        key = id,
                                        value = SavedRepository.SaveState.Set(
                                            saved = saveState.saved,
                                            saveTime = saveState.savedTime,
                                        ),
                                    )
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
                            val remoteStart = CurrentTime.mark
                            val remote = try {
                                fetchIsSaved(ids = idsToLoadFromRemote)
                            } catch (cancellationException: CancellationException) {
                                throw cancellationException
                            } catch (throwable: Throwable) {
                                for (id in idsToLoadFromRemote) {
                                    savedStates.updateValue(id, SavedRepository.SaveState.Error(throwable))
                                }

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

                            if (remote != null) {
                                val saveCheckTime = remoteStart.midpointInstantToNow()

                                idsToLoadFromRemote.zipEach(remote) { id, saved ->
                                    savedStates.updateValue(id, SavedRepository.SaveState.Set(saved))
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
                                            savedCheckTime = saveCheckTime,
                                        )
                                    }
                                } catch (cancellationException: CancellationException) {
                                    throw cancellationException
                                } catch (throwable: Throwable) {
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
        val saveTime = CurrentTime.instant

        // TODO prevent concurrent updates to saved state for the same id
        val requestLog = RequestLog(log = mutableLog)
        scope.launch {
            savedStates.updateValue(id, SavedRepository.SaveState.Setting(saved))

            val remoteStart = CurrentTime.mark
            try {
                pushSaved(ids = listOf(id), saved = saved)
            } catch (throwable: Throwable) {
                savedStates.updateValue(id, SavedRepository.SaveState.Error(throwable))
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

            val dbStart = CurrentTime.mark
            try {
                KotifyDatabase[DB.CACHE].transaction("set saved state for $entityName $id") {
                    savedEntityTable.setSaved(
                        entityId = id,
                        userId = userRepository.requireCurrentUserId,
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

            savedStates.updateValue(id, SavedRepository.SaveState.Set(saved, saveTime))

            libraryResource.update { library ->
                library.copy(ids = library.ids.plusOrMinus(value = id, condition = saved))
            }

            requestLog.success("set saved state of $entityName $id to $saved", DataSource.REMOTE)
        }
    }

    final override fun invalidateUser() {
        libraryResource.invalidate()
        savedStates.clear()
    }

    private suspend fun getLibraryCached(): SavedRepository.Library? {
        if (!userRepository.hasCurrentUserId) return null
        val userId = userRepository.requireCurrentUserId

        val requestLog = RequestLog(log = mutableLog)
        val dbStart = CurrentTime.mark
        return try {
            KotifyDatabase[DB.CACHE].transaction("load $entityName saved library") {
                GlobalUpdateTimesRepository.updated(currentUserLibraryUpdateKey)?.let { updatedTime ->
                    val library = savedEntityTable.savedEntityIds(userId = userId)
                        .map { id -> id to savedEntityTable.savedTime(entityId = id, userId = userId) }

                    updatedTime to library
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
            ?.let { (updatedTime, library) ->
                val ids = library.mapTo(mutableSetOf()) { it.first }

                requestLog.addDbTime(dbStart.elapsedNow())

                for ((id, saveTime) in library) {
                    savedStates.updateValue(id, SavedRepository.SaveState.Set(saved = true, saveTime = saveTime))
                }

                requestLog.success("loaded $entityName saved library from database", DataSource.DATABASE)
                SavedRepository.Library(ids, updatedTime)
            }
    }

    private suspend fun getLibraryRemote(): SavedRepository.Library? {
        val requestLog = RequestLog(log = mutableLog)
        val userId = userRepository.currentUserId.value ?: return null

        val remoteStart = CurrentTime.mark

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

        val dbStart = CurrentTime.mark
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

                remoteLibraryIds.map { id ->
                    id to savedEntityTable.savedTime(entityId = id, userId = userId)
                }
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

        for ((id, saveTime) in remoteLibrary) {
            savedStates.updateValue(id, SavedRepository.SaveState.Set(saved = true, saveTime = saveTime))
        }

        requestLog.success("loaded $entityName saved library from remote", DataSource.REMOTE)

        return SavedRepository.Library(remoteLibrary.mapTo(mutableSetOf()) { it.first }, fetchTime)
    }
}
