package com.dzirbel.kotify.cache

import com.dzirbel.kotify.util.zipEach

/**
 * Manages access of entities [E] referenced by String IDs between a local cache and a remote, network-based source.
 */
interface Repository<E> {
    /**
     * Retrieves the entity with the given [id] in the local cache, if it exists.
     */
    suspend fun getCached(id: String): E? = getCached(ids = listOf(id))[0]

    /**
     * Retrieves the entities with the given [ids] in the local cache, if they exist.
     *
     * The returned list has the same order and length as [ids] with missing values as null values in the list.
     */
    suspend fun getCached(ids: Iterable<String>): List<E?>

    /**
     * Retrieves the entity from the remote source for the given [id] without checking for a locally cached version,
     * saves it in the cache, and returns it.
     */
    suspend fun getRemote(id: String): E? = getRemote(ids = listOf(id))[0]

    /**
     * Retrieves the entities from the the remote source for the given [ids] without checking for locally cached
     * versions, saves them in the cache, and returns them.
     *
     * The returned list has the same order and length as [ids] with missing values as null values in the list.
     */
    suspend fun getRemote(ids: List<String>): List<E?>

    /**
     * Retrieves the entity for the given [id], from the local cache if present and it satisfies [cachePredicate],
     * otherwise fetches it from the remote source, caches, and returns it.
     */
    suspend fun get(id: String, cachePredicate: (id: String, cached: E) -> Boolean = { _, _ -> true }): E? {
        return getCached(id = id)?.takeIf { cachePredicate(id, it) } ?: getRemote(id = id)
    }

    /**
     * Retrieves the entities for the given [ids], from the local cache if present and each satisfies
     * [cachePredicate], otherwise fetches the IDs not locally cached from the remote source, caches, and returns them.
     */
    suspend fun get(
        ids: List<String>,
        cachePredicate: (id: String, cached: E) -> Boolean = { _, _ -> true },
    ): List<E?> {
        val missingIndices = ArrayList<IndexedValue<String>>()

        val cachedValues = getCached(ids = ids)
            .mapIndexedTo(ArrayList(ids.size)) { index, cached ->
                val id = ids[index]
                val result = cached?.takeIf { cachePredicate(id, it) }

                if (result == null) {
                    missingIndices.add(IndexedValue(index = index, value = id))
                }

                result
            }

        if (missingIndices.isEmpty()) {
            return cachedValues
        }

        val remote = getRemote(ids = missingIndices.map { it.value })
        missingIndices.zipEach(remote) { indexedValue, value ->
            cachedValues[indexedValue.index] = value
        }

        return cachedValues
    }

    /**
     * Invalidates the entity with the given [id], returning true if it existed and was invalidated or false otherwise.
     */
    suspend fun invalidate(id: String): Boolean = invalidate(ids = listOf(id))[0]

    /**
     * Invalidates the entities with the given [ids], returning true for each if it existed and was invalidates or false
     * otherwise.
     */
    suspend fun invalidate(ids: Iterable<String>): List<Boolean>
}
