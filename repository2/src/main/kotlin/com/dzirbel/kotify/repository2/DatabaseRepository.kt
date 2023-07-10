package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.network.model.SpotifyObject
import com.dzirbel.kotify.repository2.util.SynchronizedWeakStateFlowMap
import com.dzirbel.kotify.util.zipEach
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// TODO expose a different type than the DB model?
abstract class DatabaseRepository<EntityType : SpotifyEntity, NetworkType : SpotifyObject>(
    private val entityClass: SpotifyEntityClass<EntityType, NetworkType>,

    /**
     * The singular name of an entity, e.g. "artist"; used in transaction names.
     */
    private val entityName: String = entityClass.table.tableName.removeSuffix("s"),
) : Repository<EntityType> {

    // TODO make private again if possible
    protected val states = SynchronizedWeakStateFlowMap<String, CacheState<EntityType>>()

    /**
     * Fetches a single network model of [NetworkType] via a remote call to the network.
     *
     * This is the remote primitive and simply fetches the network model but does not cache it, unlike [getRemote].
     */
    protected abstract suspend fun fetch(id: String): NetworkType?

    /**
     * Fetches a batch of network models. By default uses iterated calls to [fetch] but implementations can provide a
     * more efficient method, i.e. in a single batched network call.
     *
     * This is the remote primitive and simply fetches the network models but does not cache them, unlike [getRemote].
     */
    protected open suspend fun fetch(ids: List<String>): List<NetworkType?> = ids.map { fetch(it) }

    override fun stateOf(id: String, cacheStrategy: CacheStrategy<EntityType>): StateFlow<CacheState<EntityType>?> {
        return states.getOrCreateStateFlow(id) {
            Repository.scope.launch {
                load(id = id, cacheStrategy = cacheStrategy)
            }
        }
    }

    override fun statesOf(
        ids: Iterable<String>,
        cacheStrategy: CacheStrategy<EntityType>,
    ): List<StateFlow<CacheState<EntityType>?>> {
        return states.getOrCreateStateFlows(keys = ids) { createdIds ->
            Repository.scope.launch {
                load(ids = createdIds, cacheStrategy = cacheStrategy)
            }
        }
    }

    override fun refreshFromRemote(id: String) {
        Repository.scope.launch {
            load(id = id, cacheStrategy = CacheStrategy.NeverValid())
        }
    }

    /**
     * Loads and updates [states] for the given [id], first from the cache and then from the remote data source if not
     * present or valid according to [cacheStrategy].
     *
     * If [cacheStrategy] is [CacheStrategy.NeverValid] then the call to the cache is skipped entirely.
     */
    private suspend fun load(id: String, cacheStrategy: CacheStrategy<EntityType>) {
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
    private suspend fun load(ids: Iterable<String>, cacheStrategy: CacheStrategy<EntityType>) {
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
    private suspend fun loadFromCache(id: String, cacheStrategy: CacheStrategy<EntityType>): Boolean {
        val cachedEntity = try {
            getCached(id = id)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Throwable) {
            // TODO log exception?
            return false
        }

        return if (cachedEntity != null && cacheStrategy.isValid(cachedEntity)) {
            states.updateValue(id, CacheState.Loaded.of(cachedEntity))
            true
        } else {
            false
        }
    }

    /**
     * Loads data for the given [ids] from the cache and applies them to [states], returning a list of IDs which were
     * NOT successfully loaded (i.e. not present in the cache or not valid according to [cacheStrategy]).
     */
    private suspend fun loadFromCache(ids: List<String>, cacheStrategy: CacheStrategy<EntityType>): List<String> {
        val cachedEntities = try {
            getCached(ids = ids)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (_: Throwable) {
            // TODO log exception?
            return ids
        }

        val missingIds = mutableListOf<String>()

        ids.zipEach(cachedEntities) { id, cachedEntity ->
            if (cachedEntity != null && cacheStrategy.isValid(cachedEntity)) {
                states.updateValue(id, CacheState.Loaded.of(cachedEntity))
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

        val remoteEntity = try {
            getRemote(id = id)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            states.updateValue(id, CacheState.Error(throwable))
            return
        }

        states.updateValue(id, CacheState.Loaded.orNotFound(remoteEntity))
    }

    /**
     * Loads data for the given [ids] from the remote data source and applies them to [states].
     */
    private suspend fun loadFromRemote(ids: List<String>) {
        for (id in ids) {
            states.updateValue(id) { CacheState.Refreshing.of(it) }
        }

        val remoteEntities = try {
            getRemote(ids = ids)
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (throwable: Throwable) {
            for (id in ids) {
                states.updateValue(id, CacheState.Error(throwable))
            }
            return
        }

        ids.zipEach(remoteEntities) { id, remoteEntity ->
            states.updateValue(id, CacheState.Loaded.orNotFound(remoteEntity))
        }
    }

    /**
     * Loads data for the given [id] from the database without updating [states].
     *
     * TODO make private again if possible
     */
    protected suspend fun getCached(id: String): EntityType? {
        return KotifyDatabase.transaction("load cached $entityName $id") { entityClass.findById(id) }
    }

    /**
     * Loads data for the given [ids] from the database without updating [states].
     */
    private suspend fun getCached(ids: List<String>): List<EntityType?> {
        return KotifyDatabase.transaction("load ${ids.count()} cached ${entityName}s") {
            entityClass.forIds(ids).toList()
        }
    }

    /**
     * Fetches data from the remote data source for [id], stores it in the database, and returns it, without updating
     * [states].
     */
    private suspend fun getRemote(id: String): EntityType? {
        return fetch(id)?.let { networkModel ->
            KotifyDatabase.transaction("save $entityName $id") { entityClass.from(networkModel) }
        }
    }

    /**
     * Fetches data from the remote data source for [ids], stores them in the database, and returns them, without
     * updating [states].
     */
    private suspend fun getRemote(ids: List<String>): List<EntityType?> {
        val networkModels = fetch(ids = ids)
        val notNullNetworkModels = networkModels.count { it != null }
        return if (notNullNetworkModels != 0) {
            KotifyDatabase.transaction("save $notNullNetworkModels ${entityName}s") {
                networkModels.map { networkModel -> networkModel?.let { entityClass.from(it) } }
            }
        } else {
            emptyList()
        }
    }

    /**
     * Determines whether this [CacheState] should be refreshed according to [cacheStrategy].
     */
    private fun <T> CacheState<T>?.needsLoad(cacheStrategy: CacheStrategy<T>): Boolean {
        return when (this) {
            is CacheState.Loaded -> cacheStrategy.isValid(cachedValue)
            is CacheState.Refreshing -> false
            is CacheState.NotFound -> false // do not retry on 404s
            is CacheState.Error -> true // always retry on errors
            null -> true
        }
    }
}
