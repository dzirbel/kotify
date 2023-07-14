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
    val uri: Column<String?> = text("uri").nullable()

    val createdTime: Column<Instant> = timestamp("created_time").clientDefault { Instant.now() }
    val updatedTime: Column<Instant> = timestamp("updated_time").clientDefault { Instant.now() }
    val fullUpdatedTime: Column<Instant?> = timestamp("full_updated_time").nullable()
}

/**
 * Base class for entity objects in a [SpotifyEntityTable].
 *
 * TODO refactor UI to avoid direct use of (unstable) database entities
 */
abstract class SpotifyEntity(id: EntityID<String>, table: SpotifyEntityTable) : Entity<String>(id) {
    var name: String by table.name
    var uri: String? by table.uri

    var createdTime: Instant by table.createdTime
    var updatedTime: Instant by table.updatedTime
    var fullUpdatedTime: Instant? by table.fullUpdatedTime
}

/**
 * Base [EntityClass] which serves as the companion object for a [SpotifyEntityTable].
 *
 * Contains common functionality to power a [DatabaseRepository] based on this table type, in particular to convert
 * network models of [NetworkType] to database models of [EntityType].
 */
abstract class SpotifyEntityClass<EntityType : SpotifyEntity, NetworkType : SpotifyObject>(table: SpotifyEntityTable) :
    EntityClass<String, EntityType>(table) {

    /**
     * Sets fields on this [EntityType] according to the given [networkModel]. Used either to create a new entity or
     * update an existing one from a new [networkModel].
     */
    protected abstract fun EntityType.update(networkModel: NetworkType)

    /**
     * Converts the given [networkModel] into an [EntityType], either creating a new entity or updating the existing one
     * based on the new network values.
     *
     * Returns null if [networkModel] has no ID.
     *
     * Must be called from within a transaction.
     */
    fun from(networkModel: NetworkType): EntityType? {
        return updateOrInsert(networkModel) { update(networkModel) }
    }

    fun updateOrInsert(networkModel: NetworkType, update: EntityType.() -> Unit): EntityType? {
        return networkModel.id?.let { id ->
            updateOrInsert(id = id, networkModel = networkModel, update = update)
        }
    }

    fun updateOrInsert(id: String, networkModel: NetworkType, update: EntityType.() -> Unit): EntityType {
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

    /**
     * Converts the given [networkModels] into [EntityType]s, either creating new entities or updating existing ones
     * based on the new network values.
     *
     * The returned list includes null values for [networkModels] with no ID.
     *
     * Must be called from within a transaction.
     */
    fun from(networkModels: List<NetworkType?>): List<EntityType?> = networkModels.map { it?.let { _ -> from(it) } }
}
