package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.DB
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.asLog
import com.dzirbel.kotify.repository.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.repository.util.midpointInstantToNow
import com.dzirbel.kotify.util.collections.zipEach
import com.dzirbel.kotify.util.coroutines.mapParallel
import com.dzirbel.kotify.util.mapFirst
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.TimeSource

abstract class DatabaseRepository<ViewModel, DatabaseType, NetworkType> internal constructor(
    protected val entityName: String,
    protected val scope: CoroutineScope,
    private val entityNamePlural: String = entityName + 's', // TODO use pluralize utils
) : Repository<ViewModel> {

    private val states = SynchronizedWeakStateFlowMap<String, CacheState<ViewModel>>()

    protected val mutableLog = MutableLog<Repository.LogData>(
        name = requireNotNull(this::class.qualifiedName).removeSuffix(".Companion").substringAfterLast('.'),
        scope = scope,
    )

    override val log = mutableLog.asLog()

    /**
     * Fetches the [DatabaseType] and the time it was last updated from the remote data source from the local database,
     * or null if there is no value for the given [id].
     *
     * Must be called from within a database transaction.
     */
    protected abstract fun fetchFromDatabase(id: String): Pair<DatabaseType, Instant>?

    /**
     * Fetches the [DatabaseType]s and the times they were last updated from the remote data source from the local
     * database, or null if there is no value for each of the given [ids].
     *
     * Must be called from within a database transaction.
     */
    protected open fun fetchFromDatabase(ids: List<String>): List<Pair<DatabaseType, Instant>?> {
        return ids.map(::fetchFromDatabase)
    }

    /**
     * Fetches the [NetworkType] for the given [id] from the remote data source.
     */
    protected abstract suspend fun fetchFromRemote(id: String): NetworkType?

    /**
     * Fetches the [NetworkType] for each of the given [ids] from the remote data source.
     */
    protected open suspend fun fetchFromRemote(ids: List<String>): List<NetworkType?> {
        return ids.mapParallel(::fetchFromRemote)
    }

    /**
     * Converts the given [networkModel] with the given [id] into a [DatabaseType], saving it in the local database.
     *
     * Must be called from within a database transaction.
     */
    abstract fun convertToDB(id: String, networkModel: NetworkType, fetchTime: Instant): DatabaseType

    /**
     * Converts the given [databaseModel] into a [ViewModel] suitable for external use.
     */
    abstract fun convertToVM(databaseModel: DatabaseType): ViewModel

    final override fun stateOf(
        id: String,
        cacheStrategy: CacheStrategy<ViewModel>,
    ): StateFlow<CacheState<ViewModel>?> {
        Repository.checkEnabled()

        val requestLog = RequestLog(log = mutableLog)
        return states.getOrCreateStateFlow(
            key = id,
            onExisting = { requestLog.info("state for $entityName $id in memory", DataSource.MEMORY) },
            onCreate = {
                scope.launch { load(id = id, cacheStrategy = cacheStrategy, requestLog = requestLog) }
            },
        )
    }

    final override fun statesOf(
        ids: Iterable<String>,
        cacheStrategy: CacheStrategy<ViewModel>,
    ): List<StateFlow<CacheState<ViewModel>?>> {
        Repository.checkEnabled()

        val requestLog = RequestLog(log = mutableLog)
        return states.getOrCreateStateFlows(
            keys = ids,
            onExisting = { numExisting ->
                requestLog.info("$numExisting/${ids.count()} $entityName states in memory", DataSource.MEMORY)
            },
            onCreate = { creations ->
                scope.launch { load(ids = creations.keys, cacheStrategy = cacheStrategy, requestLog = requestLog) }
            },
        )
    }

    final override fun refreshFromRemote(id: String): Job {
        Repository.checkEnabled()

        val requestLog = RequestLog(log = mutableLog)
        return scope.launch { load(id = id, cacheStrategy = CacheStrategy.NeverValid(), requestLog = requestLog) }
    }

    /**
     * Loads and updates [states] for the given [id], first from the cache and then from the remote data source if not
     * present or valid according to [cacheStrategy].
     *
     * If [cacheStrategy] is [CacheStrategy.NeverValid] then the call to the cache is skipped entirely.
     */
    private suspend fun load(id: String, cacheStrategy: CacheStrategy<ViewModel>, requestLog: RequestLog) {
        // TODO need this check? should not be true when either creating a new state or refreshing from remote
        if (states.getValue(id).needsLoad(cacheStrategy)) {
            val allowCache = cacheStrategy !is CacheStrategy.NeverValid

            val loadedFromCache = allowCache &&
                loadFromCache(id = id, cacheStrategy = cacheStrategy, requestLog = requestLog)

            if (!loadedFromCache) {
                loadFromRemote(id = id, requestLog = requestLog)
            }
        }
    }

    /**
     * Loads and updates [states] for the given [ids], first from the cache and then from the remote data source for any
     * entities not present in the cache or valid according to [cacheStrategy].
     *
     * If [cacheStrategy] is [CacheStrategy.NeverValid] then the call to the cache is skipped entirely.
     */
    private suspend fun load(ids: Iterable<String>, cacheStrategy: CacheStrategy<ViewModel>, requestLog: RequestLog) {
        val idsToLoad = ids.filter { id ->
            states.getValue(id).needsLoad(cacheStrategy)
        }

        if (idsToLoad.isNotEmpty()) {
            val allowCache = cacheStrategy !is CacheStrategy.NeverValid
            val idsToLoadFromRemote = if (allowCache) {
                loadFromCache(ids = idsToLoad, cacheStrategy = cacheStrategy, requestLog = requestLog)
            } else {
                idsToLoad
            }

            if (idsToLoadFromRemote.isNotEmpty()) {
                loadFromRemote(ids = idsToLoadFromRemote, requestLog = requestLog)
            }
        }
    }

    /**
     * Loads data for the given [id] from the cache and applies it to [states], returning true if the value was present
     * and does NOT need to be refreshed according to [cacheStrategy] or false otherwise.
     */
    private suspend fun loadFromCache(
        id: String,
        cacheStrategy: CacheStrategy<ViewModel>,
        requestLog: RequestLog,
    ): Boolean {
        val dbStart = TimeSource.Monotonic.markNow()

        val (viewModel, lastUpdated) = try {
            KotifyDatabase[DB.CACHE].transaction(name = "load $entityName $id") {
                fetchFromDatabase(id = id)?.mapFirst(::convertToVM)
            }
                ?: return false
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            requestLog
                .addDbTime(dbStart.elapsedNow())
                .error("error loading $entityName $id from database", DataSource.DATABASE, throwable)

            return false
        }

        requestLog.addDbTime(dbStart.elapsedNow())

        val cacheValidity = cacheStrategy.validity(viewModel)
        if (cacheValidity.canBeUsed) {
            states.updateValue(id, CacheState.Loaded(viewModel, lastUpdated))
            requestLog.success("loaded $entityName $id from database", DataSource.DATABASE)
        }
        return !cacheValidity.shouldBeRefreshed
    }

    /**
     * Loads data for the given [ids] from the cache and applies them to [states], returning a list of IDs which were
     * NOT successfully loaded (i.e. not present in the cache or need to be refreshed according to [cacheStrategy]).
     */
    private suspend fun loadFromCache(
        ids: List<String>,
        cacheStrategy: CacheStrategy<ViewModel>,
        requestLog: RequestLog,
    ): List<String> {
        val dbStart = TimeSource.Monotonic.markNow()

        val cachedEntities = try {
            KotifyDatabase[DB.CACHE].transaction(name = "load ${ids.size} $entityNamePlural") {
                fetchFromDatabase(ids = ids).map { it?.mapFirst(::convertToVM) }
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            requestLog
                .addDbTime(dbStart.elapsedNow())
                .error("error loading ${ids.size} $entityNamePlural from database", DataSource.DATABASE, throwable)

            return ids
        }

        requestLog.addDbTime(dbStart.elapsedNow())

        val missingIds = mutableListOf<String>()
        var numUsable = 0

        ids.zipEach(cachedEntities) { id, pair ->
            if (pair != null) {
                val (viewModel, lastUpdated) = pair
                val cacheValidity = cacheStrategy.validity(viewModel)
                if (cacheValidity.canBeUsed) {
                    states.updateValue(id, CacheState.Loaded(viewModel, lastUpdated))
                    numUsable++
                }
                if (cacheValidity.shouldBeRefreshed) {
                    missingIds.add(id)
                }
            } else {
                missingIds.add(id)
            }
        }

        if (numUsable > 0) {
            requestLog.success("loaded $numUsable/${ids.size} $entityNamePlural from database", DataSource.DATABASE)
        }

        if (missingIds.isNotEmpty()) {
            requestLog.info("missing ${missingIds.size}/${ids.size} $entityNamePlural in database", DataSource.DATABASE)
        }

        return missingIds
    }

    /**
     * Loads data for the given [id] from the remote data source and applies it to [states].
     */
    private suspend fun loadFromRemote(id: String, requestLog: RequestLog) {
        // TODO do not set to refreshing if existing value is valid?
        states.updateValue(id) { CacheState.Refreshing.of(it) }

        val remoteStart = TimeSource.Monotonic.markNow()

        val networkModel = try {
            fetchFromRemote(id)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            requestLog.addRemoteTime(remoteStart.elapsedNow())
            states.updateValue(id, CacheState.Error(throwable))
            requestLog.error("error loading $entityName $id from remote", DataSource.REMOTE, throwable)
            return
        }

        requestLog.addRemoteTime(remoteStart.elapsedNow())

        if (networkModel != null) {
            val fetchTime = remoteStart.midpointInstantToNow()

            val dbStart = TimeSource.Monotonic.markNow()
            val viewModel = try {
                KotifyDatabase[DB.CACHE].transaction("save $entityName $id") {
                    val databaseModel = convertToDB(id = id, networkModel = networkModel, fetchTime = fetchTime)
                    convertToVM(databaseModel = databaseModel)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                requestLog.addDbTime(dbStart.elapsedNow())
                states.updateValue(id, CacheState.Error(throwable))
                requestLog.error("error saving $entityName $id in database", DataSource.DATABASE, throwable)
                return
            }

            requestLog.addDbTime(dbStart.elapsedNow())

            if (viewModel == null) {
                states.updateValue(id, CacheState.NotFound())
                requestLog.warn("failed to convert remote $entityName $id to view model", DataSource.REMOTE)
            } else {
                states.updateValue(id, CacheState.Loaded(viewModel, fetchTime))
                requestLog.success("loaded $entityName $id from remote", DataSource.REMOTE)
            }
        } else {
            states.updateValue(id, CacheState.NotFound())
            requestLog.warn("no remote model for $entityName $id", DataSource.REMOTE)
        }
    }

    /**
     * Loads data for the given [ids] from the remote data source and applies them to [states].
     */
    private suspend fun loadFromRemote(ids: List<String>, requestLog: RequestLog) {
        // TODO do not set to refreshing if existing values are valid?
        for (id in ids) {
            states.updateValue(id) { CacheState.Refreshing.of(it) }
        }

        val remoteStart = TimeSource.Monotonic.markNow()

        val networkModels = try {
            fetchFromRemote(ids)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            requestLog.addRemoteTime(remoteStart.elapsedNow())
            for (id in ids) {
                states.updateValue(id, CacheState.Error(throwable))
            }
            requestLog.error("error loading ${ids.size} $entityNamePlural from remote", DataSource.REMOTE, throwable)
            return
        }

        requestLog.addRemoteTime(remoteStart.elapsedNow())
        val fetchTime = remoteStart.midpointInstantToNow()

        val numNotNullNetworkModels = networkModels.count { it != null }
        val numNullNetworkModels = networkModels.size - numNotNullNetworkModels

        if (numNotNullNetworkModels > 0) {
            val dbStart = TimeSource.Monotonic.markNow()
            var numNonNullViewModels = 0
            val viewModels = try {
                KotifyDatabase[DB.CACHE].transaction("save $numNotNullNetworkModels $entityNamePlural") {
                    networkModels.zip(ids) { networkModel, id ->
                        networkModel?.let {
                            val databaseModel = convertToDB(id = id, networkModel = networkModel, fetchTime = fetchTime)
                            convertToVM(databaseModel = databaseModel)
                                ?.also { numNonNullViewModels++ }
                        }
                    }
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                requestLog.addDbTime(dbStart.elapsedNow())
                for (id in ids) {
                    states.updateValue(id, CacheState.Error(throwable))
                }
                requestLog.error(
                    title = "error saving $numNotNullNetworkModels $entityNamePlural in database",
                    source = DataSource.DATABASE,
                    throwable = throwable,
                )
                return
            }

            requestLog.addDbTime(dbStart.elapsedNow())

            ids.zipEach(viewModels) { id, viewModel ->
                states.updateValue(id, viewModel?.let { CacheState.Loaded(it, fetchTime) } ?: CacheState.NotFound())
            }

            if (numNonNullViewModels > 0) {
                requestLog.success(
                    title = "loaded $numNonNullViewModels/${ids.size} $entityNamePlural from remote",
                    source = DataSource.REMOTE,
                )
            }

            val numNullViewModels = ids.size - numNonNullViewModels
            if (numNullViewModels > 0) {
                requestLog.warn(
                    title = "failed to convert $numNullViewModels/${ids.size} remote $entityNamePlural to view models",
                    source = DataSource.REMOTE,
                )
            }
        } else {
            for (id in ids) {
                states.updateValue(id, CacheState.NotFound())
            }
        }

        if (numNullNetworkModels > 0) {
            requestLog.warn(
                title = "no remote model for $numNullNetworkModels/${ids.size} $entityNamePlural",
                source = DataSource.REMOTE,
            )
        }
    }

    /**
     * Determines whether this [CacheState] should be refreshed according to [cacheStrategy].
     */
    private fun CacheState<ViewModel>?.needsLoad(cacheStrategy: CacheStrategy<ViewModel>): Boolean {
        return when (this) {
            is CacheState.Loaded -> cacheStrategy.validity(cachedValue).shouldBeRefreshed
            is CacheState.Refreshing -> false
            is CacheState.NotFound -> false // do not retry on 404s
            is CacheState.Error -> true // always retry on errors
            null -> true
        }
    }
}
