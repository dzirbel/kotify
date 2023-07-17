package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.repository2.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.repository2.util.midpointInstantToNow
import com.dzirbel.kotify.util.mapParallel
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.time.TimeSource

// TODO expose a different type than the DB model?
abstract class DatabaseRepository<DatabaseType, NetworkType> internal constructor(
    protected val entityName: String,
    protected val scope: CoroutineScope,
) : Repository<DatabaseType> {

    private val states = SynchronizedWeakStateFlowMap<String, CacheState<DatabaseType>>()

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
    abstract fun convert(id: String, networkModel: NetworkType): DatabaseType

    open fun convert(ids: List<String>, networkModels: List<NetworkType?>): List<DatabaseType?> {
        return networkModels.zip(ids) { networkModel, id ->
            networkModel?.let { convert(id, it) }
        }
    }

    final override fun stateOf(
        id: String,
        cacheStrategy: CacheStrategy<DatabaseType>,
    ): StateFlow<CacheState<DatabaseType>?> {
        return states.getOrCreateStateFlow(id) {
            scope.launch { load(id = id, cacheStrategy = cacheStrategy) }
        }
    }

    final override fun statesOf(
        ids: Iterable<String>,
        cacheStrategy: CacheStrategy<DatabaseType>,
    ): List<StateFlow<CacheState<DatabaseType>?>> {
        return states.getOrCreateStateFlows(keys = ids) { creations ->
            scope.launch { load(ids = creations.keys, cacheStrategy = cacheStrategy) }
        }
    }

    final override fun refreshFromRemote(id: String): Job {
        return scope.launch { load(id = id, cacheStrategy = CacheStrategy.NeverValid()) }
    }

    /**
     * Loads and updates [states] for the given [id], first from the cache and then from the remote data source if not
     * present or valid according to [cacheStrategy].
     *
     * If [cacheStrategy] is [CacheStrategy.NeverValid] then the call to the cache is skipped entirely.
     */
    private suspend fun load(id: String, cacheStrategy: CacheStrategy<DatabaseType>) {
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
    private suspend fun load(ids: Iterable<String>, cacheStrategy: CacheStrategy<DatabaseType>) {
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
     * Loads data for the given [id] from the cache and applies it to [states], returning true if successful (i.e. the
     * value was present in the cache and valid according to [cacheStrategy]) or false otherwise.
     */
    private suspend fun loadFromCache(id: String, cacheStrategy: CacheStrategy<DatabaseType>): Boolean {
        val pair = try {
            KotifyDatabase.transaction(name = "load $entityName $id") {
                fetchFromDatabase(id = id)
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Throwable) {
            // TODO log exception?
            return false
        }

        return if (pair != null && cacheStrategy.isValid(pair.first)) {
            states.updateValue(id, CacheState.Loaded(pair.first, pair.second))
            true
        } else {
            false
        }
    }

    /**
     * Loads data for the given [ids] from the cache and applies them to [states], returning a list of IDs which were
     * NOT successfully loaded (i.e. not present in the cache or not valid according to [cacheStrategy]).
     */
    private suspend fun loadFromCache(ids: List<String>, cacheStrategy: CacheStrategy<DatabaseType>): List<String> {
        val cachedEntities = try {
            KotifyDatabase.transaction(name = "load ${ids.size} ${entityName}s") {
                fetchFromDatabase(ids = ids)
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Throwable) {
            // TODO log exception?
            return ids
        }

        val missingIds = mutableListOf<String>()

        ids.zipEach(cachedEntities) { id, pair ->
            if (pair != null && cacheStrategy.isValid(pair.first)) {
                states.updateValue(id, CacheState.Loaded(pair.first, pair.second))
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
                KotifyDatabase.transaction("save $entityName $id") { convert(id, networkModel) }
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
                    convert(ids, networkModels)
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
    private fun <T> CacheState<T>?.needsLoad(cacheStrategy: CacheStrategy<T>): Boolean {
        return when (this) {
            is CacheState.Loaded -> !cacheStrategy.isValid(cachedValue)
            is CacheState.Refreshing -> false
            is CacheState.NotFound -> false // do not retry on 404s
            is CacheState.Error -> true // always retry on errors
            null -> true
        }
    }
}
