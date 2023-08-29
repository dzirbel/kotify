package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.model.ImageSize
import com.dzirbel.kotify.db.model.ImageTable
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column

/**
 * A simple interface for ViewModels which expose a set of images, typically implemented via [EntityImageViewModel].
 */
interface ImageViewModel {
    /**
     * Retrieves a [LazyTransactionStateFlow] reflecting the image URL most appropriate for the given [size] (typically
     * the smallest image with dimensions larger than or equal to [size]).
     */
    fun imageUrlFor(size: ImageSize): LazyTransactionStateFlow<String>
}

/**
 * An implementation of [ImageViewModel] based on a [SpotifyEntity] and a join table with [ImageTable].
 *
 * @see ImageTable.smallestLargerThan
 */
class EntityImageViewModel(
    private val entityId: String,
    private val entityName: String,
    private val imageJoinColumn: Column<EntityID<String>>,
) : ImageViewModel {
    private val imageFlowsBySize = mutableMapOf<ImageSize, LazyTransactionStateFlow<String>>()

    constructor(
        entity: SpotifyEntity,
        entityTable: SpotifyEntityTable,
        imageJoinColumn: Column<EntityID<String>>,
    ) : this(
        entityId = entity.id.value,
        entityName = entityTable.entityName,
        imageJoinColumn = imageJoinColumn,
    )

    override fun imageUrlFor(size: ImageSize): LazyTransactionStateFlow<String> {
        return imageFlowsBySize.getOrPut(size) {
            LazyTransactionStateFlow("$entityName $entityId image for $size") {
                ImageTable.smallestLargerThan(joinColumn = imageJoinColumn, id = entityId, size = size)
            }
        }
    }
}
