package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.network.model.SpotifyObject
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.dao.EntityClass
import java.time.Instant

/**
 * A [DatabaseRepository] with a [SpotifyEntity] as the local database entity.
 */
abstract class DatabaseEntityRepository<EntityType : SpotifyEntity, NetworkType : SpotifyObject> internal constructor(
    private val entityClass: EntityClass<String, EntityType>,
    entityName: String = entityClass.table.tableName.removeSuffix("s"),
    scope: CoroutineScope,
) : DatabaseRepository<EntityType, NetworkType>(entityName = entityName, scope = scope) {

    /**
     * Convenience variant of [convert] which uses the [networkModel]'s ID field, if it is non-null.
     *
     * Repositories whose [NetworkType]s always have an ID can override this to provide a non-null return type.
     */
    open fun convert(networkModel: NetworkType): EntityType? {
        return networkModel.id?.let { convert(id = it, networkModel = networkModel) }
    }

    final override fun fetchFromDatabase(id: String): Pair<EntityType, Instant>? {
        return entityClass.findById(id)?.let { entity -> entity to entity.updatedTime }
    }

    final override fun fetchFromDatabase(ids: List<String>): List<Pair<EntityType, Instant>?> {
        return ids.map { id ->
            entityClass.findById(id)?.let { entity -> entity to entity.updatedTime }
        }
    }
}
