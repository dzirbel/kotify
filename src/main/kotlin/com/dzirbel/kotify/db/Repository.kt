package com.dzirbel.kotify.db

import com.dzirbel.kotify.network.model.SpotifyObject
import com.dzirbel.kotify.util.zipEach

/**
 * Manages access of values in a local, database-based source and a remote, network-based source.
 *
 * The database models of [EntityType] are associated managed by [entityClass] while implementations must override
 * [fetch] to provide network models of [NetworkType]. The [Repository] then contains common logic for switching between
 * these sources (e.g. [getCached] or [getRemote]) and batching.
 */
abstract class Repository<EntityType : SpotifyEntity, NetworkType : SpotifyObject>(
    private val entityClass: SpotifyEntityClass<EntityType, NetworkType>,
) {
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

    /**
     * Retrieves the [EntityType] with the given [id] in the local cache, if it exists.
     */
    suspend fun getCached(id: String): EntityType? {
        return KotifyDatabase.transaction { entityClass.findById(id) }
    }

    /**
     * Retrieves the [EntityType]s with the given [ids] in the local cache, if they exist. The returned list has the
     * same order and length as [ids] with missing values as null values in the list.
     */
    suspend fun getCached(ids: Iterable<String>): List<EntityType?> {
        return KotifyDatabase.transaction {
            ids.map { id -> entityClass.findById(id) }
        }
    }

    /**
     * Retrieves the [NetworkType] from the remote source for the given [id] without checking for a locally cached
     * version, maps it to an [EntityType], saves it in the cache, and returns it.
     */
    suspend fun getRemote(id: String): EntityType? {
        return fetch(id)?.let { networkModel ->
            KotifyDatabase.transaction { entityClass.from(networkModel) }
        }
    }

    /**
     * Retrieves the [NetworkType]s from the the remote source for the given [ids] without checking for locally cached
     * versions, maps them to [EntityType]s, saves them in the cache, and returns them.
     */
    @Suppress("ReturnCount")
    suspend fun getRemote(ids: List<String>): List<EntityType?> {
        if (ids.isEmpty()) return emptyList()
        if (ids.size == 1) return listOf(getRemote(id = ids.first()))

        val networkModels = fetch(ids = ids).filterNotNull()
        if (networkModels.isEmpty()) return emptyList()

        return KotifyDatabase.transaction { entityClass.from(networkModels) }
    }

    /**
     * Retrieves the [EntityType] for the given [id], from the local cache if present and it satisfies [cachePredicate],
     * otherwise fetches it from the remote source, caches, and returns it.
     */
    suspend fun get(
        id: String,
        cachePredicate: (id: String, cached: EntityType) -> Boolean = { _, _ -> true },
    ): EntityType? {
        return getCached(id = id)?.takeIf { cachePredicate(id, it) } ?: getRemote(id = id)
    }

    /**
     * Retrieves the [EntityType]s for the given [ids], from the local cache if present and it satisfies
     * [cachePredicate], otherwise fetches the IDs not locally cached from the remote source, caches, and returns them.
     */
    suspend fun get(
        ids: List<String>,
        cachePredicate: (id: String, cached: EntityType) -> Boolean = { _, _ -> true },
    ): List<EntityType?> {
        val missingIndices = ArrayList<IndexedValue<String>>()
        val cached = ArrayList<EntityType?>(ids.size)

        // TODO use batch getCached() to avoid a new transaction on each call
        for (indexedValue in ids.withIndex()) {
            val cachedValue = getCached(id = indexedValue.value)
                ?.takeIf { cachePredicate(indexedValue.value, it) }

            cached.add(cachedValue)

            if (cachedValue == null) {
                missingIndices.add(indexedValue)
            }
        }

        if (missingIndices.isEmpty()) {
            return cached
        }

        val remote = getRemote(ids = missingIndices.map { it.value })
        missingIndices.zipEach(remote) { indexedValue, value ->
            cached[indexedValue.index] = value
        }

        return cached
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

    /**
     * Adds the given [networkModel] to the database and returns its mapped [EntityType]. Useful when an unrelated
     * endpoint also returns a model of [NetworkType].
     *
     * Must be called from within a transaction.
     */
    fun put(networkModel: NetworkType): EntityType? {
        return entityClass.from(networkModel)
    }

    /**
     * Adds the given [networkModels] to the database and returns their mapped [EntityType]s. Useful when an unrelated
     * endpoint also returns models of [NetworkType].
     *
     * Must be called from within a transaction.
     */
    fun put(networkModels: List<NetworkType>): List<EntityType?> {
        return networkModels.map { entityClass.from(it) }
    }

    /**
     * Invalidates the entity with the given [id], returning true if it existed and was invalidated or false otherwise.
     */
    suspend fun invalidate(id: String): Boolean {
        return KotifyDatabase.transaction { entityClass.findById(id)?.delete() } != null
    }

    /**
     * Invalidates the entities with the given [ids], returning true for each if it existed and was invalidates or false
     * otherwise.
     */
    suspend fun invalidate(ids: List<String>): List<Boolean> {
        return KotifyDatabase.transaction {
            ids.map { id -> entityClass.findById(id)?.delete() != null }
        }
    }
}
