package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.select
import java.time.Instant

object PlaylistTable : SpotifyEntityTable(name = "playlists") {
    private const val SNAPSHOT_ID_LENGTH = 128

    val collaborative: Column<Boolean> = bool("collaborative")
    val description: Column<String?> = text("description").nullable()
    val owner: Column<EntityID<String>> = reference("owner", UserTable)
    val public: Column<Boolean?> = bool("public").nullable()
    val snapshotId: Column<String> = varchar("snapshotId", length = SNAPSHOT_ID_LENGTH)
    val followersTotal: Column<Int?> = integer("followers_total").nullable()
    val totalTracks: Column<Int?> = integer("total_tracks").nullable()
    val tracksFetched: Column<Instant?> = timestamp("tracks_fetched_time").nullable()

    fun tracksFetchTime(playlistId: String): Instant? {
        // TODO also compare totalTracks to number of tracks in DB?
        return slice(tracksFetched)
            .select { PlaylistTable.id eq playlistId }
            .limit(1)
            .firstOrNull()
            ?.get(tracksFetched)
    }

    object PlaylistImageTable : Table() {
        val playlist = reference("playlist", PlaylistTable)
        val image = reference("image", ImageTable)
        override val primaryKey = PrimaryKey(playlist, image)
    }

    object SavedPlaylistsTable : SavedEntityTable(name = "saved_playlists")
}

class Playlist(id: EntityID<String>) : SpotifyEntity(id = id, table = PlaylistTable) {
    var collaborative: Boolean by PlaylistTable.collaborative
    var description: String? by PlaylistTable.description
    var ownerId: EntityID<String> by PlaylistTable.owner
    var public: Boolean? by PlaylistTable.public
    var snapshotId: String by PlaylistTable.snapshotId
    var followersTotal: Int? by PlaylistTable.followersTotal
    var totalTracks: Int? by PlaylistTable.totalTracks
    var tracksFetched: Instant? by PlaylistTable.tracksFetched

    var owner: User by User referencedOn PlaylistTable.owner

    var images: SizedIterable<Image> by Image via PlaylistTable.PlaylistImageTable

    companion object : SpotifyEntityClass<Playlist>(PlaylistTable)
}
