package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.network.model.SpotifyObject
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.dao.EntityClass
import java.time.Instant

/**
 * A [DatabaseRepository] with a [SpotifyEntity] as the local database entity.
 */
@Suppress("TypeParameterListSpacing") // no wrapping option appears to satisfy linting
abstract class DatabaseEntityRepository<ViewModel, EntityType : SpotifyEntity, NetworkType : SpotifyObject>
internal constructor(
    private val entityClass: EntityClass<String, EntityType>,
    entityName: String = entityClass.table.tableName.removeSuffix("s"),
    scope: CoroutineScope,
) : DatabaseRepository<ViewModel, EntityType, NetworkType>(entityName = entityName, scope = scope) {

    /**
     * Convenience variant of [convertToDB] which uses the [networkModel]'s ID field, if it is non-null.
     *
     * Repositories whose [NetworkType]s always have an ID can override this to provide a non-null return type.
     */
    open fun convertToDB(networkModel: NetworkType): EntityType? {
        return networkModel.id?.let { convertToDB(id = it, networkModel = networkModel) }
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
