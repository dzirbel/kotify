package com.dzirbel.kotify.repository2

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

    final override fun fetchFromDatabase(id: String): Pair<EntityType, Instant>? {
        return entityClass.findById(id)?.let { entity -> entity to entity.updatedTime }
    }

    final override fun fetchFromDatabase(ids: List<String>): List<Pair<EntityType, Instant>?> {
        return ids.map { id ->
            entityClass.findById(id)?.let { entity -> entity to entity.updatedTime }
        }
    }
}
