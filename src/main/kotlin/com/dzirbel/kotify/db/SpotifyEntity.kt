package com.dzirbel.kotify.db

import com.dzirbel.kotify.network.model.SpotifyObject
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Base class for tables which contain a [SpotifyEntity], and provides common columns like [name].
 */
abstract class SpotifyEntityTable(name: String = "") : StringIdTable(name = name) {
    val name: Column<String> = text("name")

    val createdTime: Column<Instant> = timestamp("created_time").clientDefault { Instant.now() }
    val updatedTime: Column<Instant> = timestamp("updated_time").clientDefault { Instant.now() }
}

/**
 * Base class for entity objects in a [SpotifyEntityTable].
 */
abstract class SpotifyEntity(id: EntityID<String>, table: SpotifyEntityTable) : Entity<String>(id) {
    var name: String by table.name

    var createdTime: Instant by table.createdTime
    var updatedTime: Instant by table.updatedTime
}

/**
 * Base [EntityClass] which serves as the companion object for a [SpotifyEntityTable].
 *
 * Contains common functionality to power a [Repository] based on this table type, in particular to convert network
 * models of [NetworkType] to database models of [EntityType].
 */
abstract class SpotifyEntityClass<EntityType : SpotifyEntity, NetworkType : SpotifyObject>(table: SpotifyEntityTable) :
    EntityClass<String, EntityType>(table) {

    /**
     * Sets fields on this [EntityType] according to the given [networkModel]. Used either to create a new entity or
     * update an existing one from a new [networkModel].
     */
    abstract fun EntityType.update(networkModel: NetworkType)

    /**
     * Converts the given [networkModel] into an [EntityType], either creating a new entity or updating the existing one
     * based on the new network values.
     *
     * Returns null if [networkModel] has no ID.
     *
     * Must be called from within a transaction.
     */
    fun from(networkModel: NetworkType): EntityType? {
        val id = networkModel.id ?: return null

        return findById(id)
            ?.apply {
                updatedTime = Instant.now()
                name = networkModel.name
                update(networkModel)
            }
            ?: new(id = id) {
                name = networkModel.name
                update(networkModel)
            }
    }

    /**
     * Converts the given [networkModels] into [EntityType]s, either creating new entities or updating existing ones
     * based on the new network values.
     *
     * The returned list includes null values for [networkModels] with no ID.
     *
     * Must be called from within a transaction.
     */
    fun from(networkModels: List<NetworkType>): List<EntityType?> = networkModels.map { from(it) }
}
