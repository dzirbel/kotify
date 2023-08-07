package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.repository.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.repository.util.midpointInstantToNow
import com.dzirbel.kotify.util.mapFirst
import com.dzirbel.kotify.util.mapParallel
import com.dzirbel.kotify.util.zipEach
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
) : Repository<ViewModel> {

    private val states = SynchronizedWeakStateFlowMap<String, CacheState<ViewModel>>()

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
    abstract fun convertToDB(id: String, networkModel: NetworkType): DatabaseType

    /**
     * Converts the given [databaseModel] into a [ViewModel] suitable for external use.
     */
    abstract fun convertToVM(databaseModel: DatabaseType): ViewModel

    final override fun stateOf(
        id: String,
        cacheStrategy: CacheStrategy<ViewModel>,
    ): StateFlow<CacheState<ViewModel>?> {
        Repository.checkEnabled()
        return states.getOrCreateStateFlow(id) {
            scope.launch { load(id = id, cacheStrategy = cacheStrategy) }
        }
    }

    final override fun statesOf(
        ids: Iterable<String>,
        cacheStrategy: CacheStrategy<ViewModel>,
    ): List<StateFlow<CacheState<ViewModel>?>> {
        Repository.checkEnabled()
        return states.getOrCreateStateFlows(keys = ids) { creations ->
            scope.launch { load(ids = creations.keys, cacheStrategy = cacheStrategy) }
        }
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
        } catch (_: Throwable) {
            // TODO log exception?
            return false
        }

        return if (viewModel != null && lastUpdated != null) {
            val cacheValidity = cacheStrategy.validity(viewModel)
            if (cacheValidity.canBeUsed) {
                states.updateValue(id, CacheState.Loaded(viewModel, lastUpdated))
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
        val cachedEntities = try {
            KotifyDatabase.transaction(name = "load ${ids.size} ${entityName}s") {
                fetchFromDatabase(ids = ids).map { it?.mapFirst(::convertToVM) }
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Throwable) {
            // TODO log exception?
            return ids
        }

        val missingIds = mutableListOf<String>()

        ids.zipEach(cachedEntities) { id, pair ->
            val viewModel = pair?.first
            if (viewModel != null) {
                val cacheValidity = cacheStrategy.validity(viewModel)
                if (cacheValidity.canBeUsed) {
                    states.updateValue(id, CacheState.Loaded(viewModel, pair.second))
                }
                if (cacheValidity.shouldBeRefreshed) {
                    missingIds.add(id)
                }
            } else {
                missingIds.add(id)
            }
        }

        return missingIds
    }

    /**
     * Loads data for the given [id] from the remote data source and applies it to [states].
     */
    private suspend fun loadFromRemote(id: String) {
        states.updateValue(id) { CacheState.Refreshing.of(it) }

        val start = TimeSource.Monotonic.markNow()
        val remoteEntity = try {
            fetchFromRemote(id)?.let { networkModel ->
                KotifyDatabase.transaction("save $entityName $id") { convertToVM(convertToDB(id, networkModel)) }
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            states.updateValue(id, CacheState.Error(throwable))
            return
        }

        val fetchTime = start.midpointInstantToNow()

        states.updateValue(id, remoteEntity?.let { CacheState.Loaded(it, fetchTime) } ?: CacheState.NotFound())
    }

    /**
     * Loads data for the given [ids] from the remote data source and applies them to [states].
     */
    private suspend fun loadFromRemote(ids: List<String>) {
        for (id in ids) {
            states.updateValue(id) { CacheState.Refreshing.of(it) }
        }

        val start = TimeSource.Monotonic.markNow()
        val remoteEntities = try {
            val networkModels = fetchFromRemote(ids)
            val notNullNetworkModels = networkModels.count { it != null }
            if (notNullNetworkModels > 0) {
                KotifyDatabase.transaction("save $notNullNetworkModels ${entityName}s") {
                    networkModels.zip(ids) { networkModel, id ->
                        networkModel?.let { convertToVM(convertToDB(id, it)) }
                    }
                }
            } else {
                emptyList()
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            for (id in ids) {
                states.updateValue(id, CacheState.Error(throwable))
            }
            return
        }

        // TODO may not be the most accurate for batch loads
        val fetchTime = start.midpointInstantToNow()

        ids.zipEach(remoteEntities) { id, remoteEntity ->
            states.updateValue(id, remoteEntity?.let { CacheState.Loaded(it, fetchTime) } ?: CacheState.NotFound())
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
