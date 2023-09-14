package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.network.model.SpotifyObject
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.dao.EntityClass
import java.time.Instant

/**
 * A [DatabaseRepository] with a [SpotifyEntity] as the local database entity.
 */
abstract class DatabaseEntityRepository<
    ViewModel : EntityViewModel,
    Entity : SpotifyEntity,
    Network : SpotifyObject,
    > internal constructor(
    private val entityClass: EntityClass<String, Entity>,
    entityName: String = entityClass.table.tableName.removeSuffix("s"),
    scope: CoroutineScope,
) : DatabaseRepository<ViewModel, Entity, Network>(entityName = entityName, scope = scope) {

    override val defaultCacheStrategy = CacheStrategy.RequiringFullEntity<ViewModel>()
        .then(CacheStrategy.EntityTTL())

    final override fun fetchFromDatabase(id: String): Pair<Entity, Instant>? {
        return entityClass.findById(id)?.let { entity -> entity to entity.updatedTime }
    }

    final override fun fetchFromDatabase(ids: List<String>): List<Pair<Entity, Instant>?> {
        return ids.map { id ->
            entityClass.findById(id)?.let { entity -> entity to entity.updatedTime }
        }
    }
}
