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

abstract class DatabaseRepository<EntityType : SpotifyEntity, NetworkType : SpotifyObject>(
    private val entityClass: SpotifyEntityClass<EntityType, NetworkType>,

    /**
     * The singular name of an entity, e.g. "artist"; used in transaction names.
     */
    private val entityName: String = entityClass.table.tableName.removeSuffix("s"),
) : Repository<EntityType> {

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

    override fun stateOf(id: String): StateFlow<CacheState<EntityType>?> {
        return states.getOrCreateStateFlow(id)
    }

    override fun ensureLoaded(id: String, cacheStrategy: CacheStrategy<EntityType>) {
        ensureLoaded(ids = listOf(id)) // TODO
    }

    @Suppress("LabeledExpression")
    override fun ensureLoaded(ids: Iterable<String>, cacheStrategy: CacheStrategy<EntityType>) {
        Repository.scope.launch {
            val idsToLoad = ids.filter { id ->
                states.getValue(id).needsLoad(cacheStrategy)
            }

            if (idsToLoad.isNotEmpty()) {
                val cachedEntities = try {
                    getCached(ids = idsToLoad)
                } catch (cancellationException: CancellationException) {
                    throw cancellationException
                } catch (throwable: Throwable) {
                    for (id in idsToLoad) {
                        states.updateValue(id, CacheState.Error(throwable))
                    }
                    return@launch
                }

                val idsToLoadFromRemote = mutableListOf<String>()

                idsToLoad.zipEach(cachedEntities) { id, cachedEntity ->
                    if (cachedEntity != null && cacheStrategy.isValid(cachedEntity)) {
                        states.updateValue(
                            id,
                            CacheState.Loaded(cachedValue = cachedEntity, cacheTime = cachedEntity.updatedTime),
                        )
                    } else {
                        idsToLoadFromRemote.add(id)
                    }
                }

                if (idsToLoadFromRemote.isNotEmpty()) {
                    for (id in idsToLoadFromRemote) {
                        // retain previous value if there is one
                        states.updateValue(id) {
                            CacheState.Refreshing(cachedValue = it?.cachedValue, cacheTime = it?.cacheTime)
                        }
                    }

                    val remoteEntities = try {
                        getRemote(ids = idsToLoadFromRemote)
                    } catch (cancellationException: CancellationException) {
                        throw cancellationException
                    } catch (throwable: Throwable) {
                        for (id in idsToLoadFromRemote) {
                            states.updateValue(id, CacheState.Error(throwable))
                        }
                        return@launch
                    }

                    idsToLoadFromRemote.zipEach(remoteEntities) { id, remoteEntity ->
                        if (remoteEntity != null) {
                            states.updateValue(
                                id,
                                CacheState.Loaded(cachedValue = remoteEntity, cacheTime = remoteEntity.updatedTime),
                            )
                        } else {
                            states.updateValue(id, CacheState.NotFound())
                        }
                    }
                }
            }
        }
    }

    protected suspend fun getCached(id: String): EntityType? {
        return KotifyDatabase.transaction("load cached $entityName $id") { entityClass.findById(id) }
    }

    private suspend fun getCached(ids: List<String>): List<EntityType?> {
        require(ids.isNotEmpty())
        return if (ids.size == 1) {
            listOf(getCached(ids.first()))
        } else {
            KotifyDatabase.transaction("load ${ids.count()} cached ${entityName}s") {
                // TODO batch?
                ids.map { id -> entityClass.findById(id) }
            }
        }
    }

    private suspend fun getRemote(id: String): EntityType? {
        return fetch(id)?.let { networkModel ->
            KotifyDatabase.transaction("save $entityName $id") { entityClass.from(networkModel) }
        }
    }

    private suspend fun getRemote(ids: List<String>): List<EntityType?> {
        require(ids.isNotEmpty())
        return if (ids.size == 1) {
            listOf(getRemote(id = ids[0]))
        } else {
            val networkModels = fetch(ids = ids)
            KotifyDatabase.transaction("save ${ids.size} ${entityName}s") { entityClass.from(networkModels) }
        }
    }

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
