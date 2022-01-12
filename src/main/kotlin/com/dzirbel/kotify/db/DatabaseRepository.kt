package com.dzirbel.kotify.db

import com.dzirbel.kotify.cache.Repository
import com.dzirbel.kotify.network.model.SpotifyObject

/**
 * A [Repository] which uses database entities [EntityType] as its local cache.
 *
 * The database models of [EntityType] are managed by [entityClass] while implementations must override [fetch] to
 * provide network models of [NetworkType].
 */
abstract class DatabaseRepository<EntityType : SpotifyEntity, NetworkType : SpotifyObject>(
    private val entityClass: SpotifyEntityClass<EntityType, NetworkType>,
) : Repository<EntityType> {
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

    final override suspend fun getCached(id: String): EntityType? {
        return KotifyDatabase.transaction { entityClass.findById(id) }
    }

    final override suspend fun getCached(ids: Iterable<String>): List<EntityType?> {
        return KotifyDatabase.transaction {
            ids.map { id -> entityClass.findById(id) }
        }
    }

    final override suspend fun getRemote(id: String): EntityType? {
        return fetch(id)?.let { networkModel ->
            KotifyDatabase.transaction { entityClass.from(networkModel) }
        }
    }

    final override suspend fun getRemote(ids: List<String>): List<EntityType?> {
        return when (ids.size) {
            0 -> emptyList()
            1 -> listOf(getRemote(id = ids[0]))
            else -> {
                val networkModels = fetch(ids = ids)
                KotifyDatabase.transaction { entityClass.from(networkModels) }
            }
        }
    }

    /**
     * Retrieves the the [EntityType] for the given [id], guaranteeing that it has a non-null
     * [SpotifyEntity.fullUpdatedTime], i.e. corresponds to a full model.
     */
    suspend fun getFull(id: String): EntityType? {
        return get(id = id, cachePredicate = { _, value -> value.fullUpdatedTime != null })
    }

    /**
     * Retrieves the the [EntityType]s for the given [ids], guaranteeing that each has a non-null
     * [SpotifyEntity.fullUpdatedTime], i.e. corresponds to a full model.
     */
    suspend fun getFull(ids: List<String>): List<EntityType?> {
        return get(ids = ids, cachePredicate = { _, value -> value.fullUpdatedTime != null })
    }

    final override suspend fun invalidate(id: String): Boolean {
        return KotifyDatabase.transaction { entityClass.findById(id)?.delete() } != null
    }

    final override suspend fun invalidate(ids: Iterable<String>): List<Boolean> {
        return KotifyDatabase.transaction {
            ids.map { id -> entityClass.findById(id)?.delete() != null }
        }
    }
}
