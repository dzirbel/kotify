package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.SpotifyEntity
import java.time.Instant

/**
 * A convenience base class for view models with common properties provided by a [SpotifyEntity].
 */
abstract class EntityViewModel(entity: SpotifyEntity) {
    val id: String = entity.id.value
    val uri: String? = entity.uri
    val name: String = entity.name

    val updatedTime: Instant = entity.updatedTime
    val fullUpdatedTime: Instant? = entity.fullUpdatedTime
}
