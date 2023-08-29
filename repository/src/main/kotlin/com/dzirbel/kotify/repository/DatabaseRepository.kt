package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.asLog
import com.dzirbel.kotify.log.error
import com.dzirbel.kotify.log.info
import com.dzirbel.kotify.log.success
import com.dzirbel.kotify.log.warn
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

    protected val mutableLog = MutableLog<Log.Event>(
        name = requireNotNull(this::class.qualifiedName).removeSuffix(".Companion").substringAfterLast('.'),
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
        return states.getOrCreateStateFlow(
            key = id,
            onExisting = {
                scope.launch { mutableLog.info("state for $entityName $id in memory") }
            },
            onCreate = {
                scope.launch { load(id = id, cacheStrategy = cacheStrategy) }
            },
        )
    }

    final override fun statesOf(
        ids: Iterable<String>,
        cacheStrategy: CacheStrategy<ViewModel>,
    ): List<StateFlow<CacheState<ViewModel>?>> {
        Repository.checkEnabled()
        return states.getOrCreateStateFlows(
            keys = ids,
            onExisting = { numExisting ->
                scope.launch { mutableLog.info("$numExisting $entityName states in memory") }
            },
            onCreate = { creations ->
                scope.launch { load(ids = creations.keys, cacheStrategy = cacheStrategy) }
            },
        )
    }

    final override fun refreshFromRemote(id: String): Job {
        Repository.checkEnabled()
        return scope.launch { load(id = id, cacheStrategy = CacheStrategy.NeverValid()) }
    }

    /**
     * Loads and updates [states] for the given [id], first from the cache and then from the remote data source if not
     * present or valid according to [cacheStrategy].
     *
     * If [cacheStrategy] is [CacheStrategy.NeverValid] then the call to the cache is skipped entirely.
     */
    private suspend fun load(id: String, cacheStrategy: CacheStrategy<ViewModel>) {
        // TODO need this check? should not be true when either creating a new state or refreshing from remote
        if (states.getValue(id).needsLoad(cacheStrategy)) {
            val allowCache = cacheStrategy !is CacheStrategy.NeverValid
            val loadedFromCache = allowCache && loadFromCache(id = id, cacheStrategy = cacheStrategy)

            if (!loadedFromCache) {
                loadFromRemote(id = id)
            }
        }
    }

    /**
     * Loads and updates [states] for the given [ids], first from the cache and then from the remote data source for any
     * entities not present in the cache or valid according to [cacheStrategy].
     *
     * If [cacheStrategy] is [CacheStrategy.NeverValid] then the call to the cache is skipped entirely.
     */
    private suspend fun load(ids: Iterable<String>, cacheStrategy: CacheStrategy<ViewModel>) {
        val idsToLoad = ids.filter { id ->
            states.getValue(id).needsLoad(cacheStrategy)
        }

        if (idsToLoad.isNotEmpty()) {
            val allowCache = cacheStrategy !is CacheStrategy.NeverValid
            val idsToLoadFromRemote = if (allowCache) {
                loadFromCache(ids = idsToLoad, cacheStrategy = cacheStrategy)
            } else {
                idsToLoad
            }

            if (idsToLoadFromRemote.isNotEmpty()) {
                loadFromRemote(idsToLoadFromRemote)
            }
        }
    }

    /**
     * Loads data for the given [id] from the cache and applies it to [states], returning true if the value was present
     * and does NOT need to be refreshed according to [cacheStrategy] or false otherwise.
     */
    private suspend fun loadFromCache(id: String, cacheStrategy: CacheStrategy<ViewModel>): Boolean {
        val start = TimeSource.Monotonic.markNow()

        val viewModel: ViewModel?
        val lastUpdated: Instant?

        try {
            KotifyDatabase.transaction(name = "load $entityName $id") {
                val pair = fetchFromDatabase(id = id)
                viewModel = pair?.first?.let { convertToVM(it) }
                lastUpdated = pair?.second
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            mutableLog.error(
                throwable = throwable,
                title = "error loading $entityName $id from database in ${start.elapsedNow()}",
            )
            return false
        }

        return if (viewModel != null && lastUpdated != null) {
            val cacheValidity = cacheStrategy.validity(viewModel)
            if (cacheValidity.canBeUsed) {
                states.updateValue(id, CacheState.Loaded(viewModel, lastUpdated))
                mutableLog.success(title = "loaded $entityName $id from database in ${start.elapsedNow()}")
            }
            !cacheValidity.shouldBeRefreshed
        } else {
            false
        }
    }

    /**
     * Loads data for the given [ids] from the cache and applies them to [states], returning a list of IDs which were
     * NOT successfully loaded (i.e. not present in the cache or need to be refreshed according to [cacheStrategy]).
     */
    private suspend fun loadFromCache(ids: List<String>, cacheStrategy: CacheStrategy<ViewModel>): List<String> {
        val start = TimeSource.Monotonic.markNow()

        val cachedEntities = try {
            KotifyDatabase.transaction(name = "load ${ids.size} $entityNamePlural") {
                fetchFromDatabase(ids = ids).map { it?.mapFirst(::convertToVM) }
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            mutableLog.error(
                throwable = throwable,
                title = "error loading ${ids.size} $entityNamePlural from database in ${start.elapsedNow()}",
            )
            return ids
        }

        val missingIds = mutableListOf<String>()
        var numUsable = 0

        ids.zipEach(cachedEntities) { id, pair ->
            val viewModel = pair?.first
            if (viewModel != null) {
                val cacheValidity = cacheStrategy.validity(viewModel)
                if (cacheValidity.canBeUsed) {
                    states.updateValue(id, CacheState.Loaded(viewModel, pair.second))
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
            mutableLog.success(title = "loaded $numUsable $entityNamePlural from database in ${start.elapsedNow()}")
        }

        return missingIds
    }

    /**
     * Loads data for the given [id] from the remote data source and applies it to [states].
     */
    private suspend fun loadFromRemote(id: String) {
        val start = TimeSource.Monotonic.markNow()

        states.updateValue(id) { CacheState.Refreshing.of(it) }

        val networkModel = try {
            fetchFromRemote(id)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            // TODO do not update to error if existing values are valid?
            states.updateValue(id, CacheState.Error(throwable))
            mutableLog.error(
                throwable = throwable,
                title = "error loading $entityName $id from remote in ${start.elapsedNow()}",
            )
            return
        }

        if (networkModel != null) {
            val fetchTime = start.midpointInstantToNow()
            val fetchDuration = start.elapsedNow()

            val dbStart = TimeSource.Monotonic.markNow()
            val viewModel = try {
                KotifyDatabase.transaction("save $entityName $id") {
                    val databaseModel = convertToDB(id = id, networkModel = networkModel, fetchTime = fetchTime)
                    convertToVM(databaseModel = databaseModel)
                }
            } catch (cancellationException: CancellationException) {
                throw cancellationException
            } catch (throwable: Throwable) {
                mutableLog.error(
                    throwable = throwable,
                    title = "error saving $entityName $id in database in ${dbStart.elapsedNow()}",
                )
                states.updateValue(id, CacheState.Error(throwable))
                return
            }

            if (viewModel == null) {
                states.updateValue(id, CacheState.NotFound())
                mutableLog.warn("failed to convert remote $entityName $id to view model")
            } else {
                states.updateValue(id, CacheState.Loaded(viewModel, fetchTime))
                mutableLog.success(
                    "loaded $entityName $id from remote in ${start.elapsedNow()} " +
                        "($fetchDuration remote; ${dbStart.elapsedNow()} in database)",
                )
            }
        } else {
            // TODO update state to 404?
        }
    }

    /**
     * Loads data for the given [ids] from the remote data source and applies them to [states].
     */
    private suspend fun loadFromRemote(ids: List<String>) {
        val start = TimeSource.Monotonic.markNow()

        for (id in ids) {
            states.updateValue(id) { CacheState.Refreshing.of(it) }
        }

        val networkModels = try {
            fetchFromRemote(ids)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            for (id in ids) {
                states.updateValue(id, CacheState.Error(throwable))
            }
            mutableLog.error(
                throwable = throwable,
                title = "error loading ${ids.size} $entityNamePlural from remote in ${start.elapsedNow()}",
            )
            return
        }

        val notNullNetworkModels = networkModels.count { it != null }
        if (notNullNetworkModels > 0) {
            val fetchTime = start.midpointInstantToNow()
            val fetchDuration = start.elapsedNow()

            val dbStart = TimeSource.Monotonic.markNow()
            var numNonNullViewModels = 0
            val viewModels = try {
                KotifyDatabase.transaction("save $notNullNetworkModels $entityNamePlural") {
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
                for (id in ids) {
                    states.updateValue(id, CacheState.Error(throwable))
                }
                mutableLog.error(
                    throwable = throwable,
                    title = "error saving ${ids.size} $entityNamePlural in database in ${dbStart.elapsedNow()}",
                )
                return
            }

            ids.zipEach(viewModels) { id, viewModel ->
                states.updateValue(id, viewModel?.let { CacheState.Loaded(it, fetchTime) } ?: CacheState.NotFound())
            }

            if (numNonNullViewModels > 0) {
                mutableLog.success(
                    "loaded $numNonNullViewModels $entityNamePlural from remote in ${start.elapsedNow()} " +
                        "($fetchDuration remote; ${dbStart.elapsedNow()} in database)",
                )
            }

            val missingViewModels = ids.size - numNonNullViewModels
            if (missingViewModels > 0) {
                mutableLog.warn("failed to convert $missingViewModels remote $entityNamePlural to view models")
            }
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
