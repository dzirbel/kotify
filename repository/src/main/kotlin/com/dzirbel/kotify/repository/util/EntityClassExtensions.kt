package com.dzirbel.kotify.repository.util

import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.network.model.SpotifyObject
import org.jetbrains.exposed.dao.EntityClass
import java.time.Instant

/**
 * Convenience function which finds and updates the [EntityType] with the given [id] or creates a new one if none
 * exists; in either case, the entity is returned.
 *
 * This consolidates logic to set common properties like [SpotifyEntity.updatedTime] and [SpotifyEntity.name] as
 * well as calling [update] in either case of finding or creating an entity.
 */
internal fun <EntityType : SpotifyEntity, NetworkType : SpotifyObject> EntityClass<String, EntityType>.updateOrInsert(
    id: String,
    networkModel: NetworkType,
    update: EntityType.() -> Unit,
): EntityType {
    return findById(id)
        ?.apply {
            updatedTime = Instant.now()
            networkModel.name?.let { name = it }
            uri = networkModel.uri
            update()
        }
        ?: new(id = id) {
            networkModel.name?.let { name = it }
            uri = networkModel.uri
            update()
        }
}
