package com.dzirbel.kotify.db

import com.dzirbel.kotify.db.model.GlobalUpdateTimesTable
import java.time.Instant

/**
 * Manages the state of entities which can be saved/unsaved from a user's library.
 *
 * Locally, individual records for each entity is saved in the [savedEntityTable] with the time the entire library was
 * synced saved in the [GlobalUpdateTimesTable] via [libraryUpdateKey].
 *
 * TODO unit test
 */
abstract class SavedRepository<SavedNetworkType>(
    private val savedEntityTable: SavedEntityTable,
    private val libraryUpdateKey: String = savedEntityTable.tableName,
) {
    /**
     * Fetches the saved state of each of the given [ids] via a remote call to the network.
     *
     * This is the remote primitive and simply fetches the network state but does not cache it, unlike [isSavedRemote].
     */
    protected abstract suspend fun fetchIsSaved(ids: List<String>): List<Boolean>

    /**
     * Updates the saved state of each of the given [ids] to [saved] via a remote call to the network.
     *
     * This is the remote primitive and simply pushes the network state but does not cache it, unlike [setSaved].
     */
    protected abstract suspend fun pushSaved(ids: List<String>, saved: Boolean)

    /**
     * Fetches the current state of the library of saved entities, i.e. all the entities which the user has saved.
     *
     * This is the remote primitive and simply fetches the network state but does not cache it, unlike [getLibrary].
     */
    protected abstract suspend fun fetchLibrary(): Iterable<SavedNetworkType>

    /**
     * Converts the given [savedNetworkType] model into the ID of the saved entity, and adds any corresponding model to
     * the database. E.g. for saved artists, the attached artist model should be inserted into/used to update the
     * database and its ID returned. Always called from within a transaction.
     */
    protected abstract fun from(savedNetworkType: SavedNetworkType): String?

    /**
     * Determines whether [id] has been saved to the user's library, from the local database cache. Returns null if its
     * status is not cached.
     */
    suspend fun isSavedCached(id: String): Boolean? {
        return KotifyDatabase.transaction { savedEntityTable.isSaved(entityId = id) }
    }

    /**
     * Determines whether each of [ids] has been saved to the user's library, from the local database cache. Returns
     * null for each if its status is not cached.
     */
    suspend fun isSavedCached(ids: List<String>): List<Boolean?> {
        return KotifyDatabase.transaction {
            ids.map { savedEntityTable.isSaved(entityId = id) }
        }
    }

    /**
     * Retrieves the saved state from the remote source for the given [id] without checking for a locally cached state,
     * saves it to the cache, and returns it.
     */
    suspend fun isSavedRemote(id: String): Boolean {
        val saved = fetchIsSaved(ids = listOf(id)).first()

        KotifyDatabase.transaction {
            savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = null)
        }

        return saved
    }

    /**
     * Retrieves the saved state for the given [id], from the local cache if present, otherwise fetches it from the
     * remote source, caches, and returns it.
     */
    suspend fun isSaved(id: String) = isSavedCached(id) ?: isSavedRemote(id)

    /**
     * Saves the entity with the given [id] to the user's library, both via a remote call and in the local cache.
     */
    suspend fun save(id: String) = setSaved(id = id, saved = true)

    /**
     * Removes the entity with the given [id] from the user's library, both via a remote call and in the local cache.
     */
    suspend fun unsave(id: String) = setSaved(id = id, saved = false)

    /**
     * Saves the entities with the given [ids] to the user's library, both via a remote call and in the local cache.
     */
    suspend fun save(ids: List<String>) = setSaved(ids = ids, saved = true)

    /**
     * Removes the entities with the given [ids] from the user's library, both via a remote call and in the local cache.
     */
    suspend fun unsave(ids: List<String>) = setSaved(ids = ids, saved = false)

    /**
     * Adds or removes the entity with the given [id] from the user's library according to the given [saved] state, both
     * via a remote call and in the local cache.
     */
    suspend fun setSaved(id: String, saved: Boolean) {
        pushSaved(ids = listOf(id), saved = saved)

        KotifyDatabase.transaction {
            savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = Instant.now())
        }
    }

    /**
     * Adds or removes the entities with the given [ids] from the user's library according to the given [saved] state,
     * both via a remote call and in the local cache.
     */
    suspend fun setSaved(ids: List<String>, saved: Boolean) {
        pushSaved(ids = ids, saved = saved)

        KotifyDatabase.transaction {
            ids.forEach { id -> savedEntityTable.setSaved(entityId = id, saved = saved, savedTime = Instant.now()) }
        }
    }

    /**
     * Returns the last time the entire library state (i.e. set of all the user's saved entities) was updated, or null
     * if it has never been fetched.
     */
    suspend fun libraryUpdated(): Instant? {
        return KotifyDatabase.transaction { GlobalUpdateTimesTable.updated(libraryUpdateKey) }
    }

    /**
     * Invalidates the library state, i.e. the set of all the user's saved entities, in the local cache.
     */
    suspend fun invalidateLibrary() {
        GlobalUpdateTimesTable.invalidate(libraryUpdateKey)
    }

    /**
     * Gets the library of saved entity IDs from the local database cache, or null if it has never been fetched in full.
     */
    suspend fun getLibraryCached(): Set<String>? {
        GlobalUpdateTimesTable.updated(libraryUpdateKey) ?: return null
        return KotifyDatabase.transaction { savedEntityTable.savedEntityIds() }
    }

    /**
     * Retrieves the library of saved entity IDs, from the database cache if it is present or from the remote network
     * otherwise.
     */
    suspend fun getLibrary(): Set<String> {
        val updated = GlobalUpdateTimesTable.updated(libraryUpdateKey)

        return if (updated == null) {
            val savedNetworkModels = fetchLibrary()

            KotifyDatabase.transaction {
                savedNetworkModels
                    .mapNotNullTo(mutableSetOf()) { from(it) }
                    .also {
                        GlobalUpdateTimesTable.setUpdated(libraryUpdateKey)
                    }
            }
        } else {
            KotifyDatabase.transaction { savedEntityTable.savedEntityIds() }
        }
    }
}
