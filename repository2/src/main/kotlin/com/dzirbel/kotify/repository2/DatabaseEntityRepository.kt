package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.network.model.SpotifyObject
import org.jetbrains.exposed.dao.EntityClass
import java.time.Instant

/**
 * A [DatabaseRepository] with a [SpotifyEntity] as the local database entity.
 */
abstract class DatabaseEntityRepository<EntityType : SpotifyEntity, NetworkType : SpotifyObject>(
    private val entityClass: EntityClass<String, EntityType>,
    entityName: String = entityClass.table.tableName.removeSuffix("s"),
) : DatabaseRepository<EntityType, NetworkType>(entityName) {

    final override fun fetchFromDatabase(id: String): Pair<EntityType, Instant>? {
        return entityClass.findById(id)?.let { entity -> entity to entity.updatedTime }
    }

    final override fun fetchFromDatabase(ids: List<String>): List<Pair<EntityType, Instant>?> {
        return ids.map { id ->
            entityClass.findById(id)?.let { entity -> entity to entity.updatedTime }
        }
    }
}
