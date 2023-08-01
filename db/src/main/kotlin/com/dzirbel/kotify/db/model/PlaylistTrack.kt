package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.ReadWriteCachedProperty
import com.dzirbel.kotify.db.cached
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import java.time.Instant

object PlaylistTrackTable : IntIdTable() {
    val addedAd: Column<String?> = varchar("added_at", 20).nullable()
    val isLocal: Column<Boolean> = bool("is_local")
    val indexOnPlaylist: Column<Int> = integer("index_on_playlist")

    val addedBy: Column<EntityID<String>> = reference("user", UserTable)
    val playlist: Column<EntityID<String>> = reference("playlist", PlaylistTable)
    val track: Column<EntityID<String>> = reference("track", TrackTable)

    init {
        uniqueIndex(playlist, track)
    }
}

class PlaylistTrack(id: EntityID<Int>) : IntEntity(id) {
    var playlistId: EntityID<String> by PlaylistTrackTable.playlist
    var trackId: EntityID<String> by PlaylistTrackTable.track

    var addedAt: String? by PlaylistTrackTable.addedAd
    var isLocal: Boolean by PlaylistTrackTable.isLocal
    var indexOnPlaylist: Int by PlaylistTrackTable.indexOnPlaylist

    val addedBy: ReadWriteCachedProperty<User> by (User referencedOn PlaylistTrackTable.addedBy).cached()
    val playlist: ReadWriteCachedProperty<Playlist> by (Playlist referencedOn PlaylistTrackTable.playlist).cached()
    val track: ReadWriteCachedProperty<Track> by (Track referencedOn PlaylistTrackTable.track).cached()

    val addedAtInstant: Instant? by lazy {
        addedAt?.let { Instant.parse(it) }
    }

    companion object : IntEntityClass<PlaylistTrack>(PlaylistTrackTable) {
        fun findOrCreate(trackId: String, playlistId: String): PlaylistTrack {
            return find { (PlaylistTrackTable.track eq trackId) and (PlaylistTrackTable.playlist eq playlistId) }
                .firstOrNull()
                ?: new {
                    this.trackId = EntityID(id = trackId, table = TrackTable)
                    this.playlistId = EntityID(id = playlistId, table = PlaylistTable)
                }
        }

        /**
         * Returns the tracks of the playlist with the given [playlistId] in the order they appear on the playlist.
         */
        fun tracksInOrder(playlistId: String): List<PlaylistTrack> {
            return find { PlaylistTrackTable.playlist eq playlistId }
                .orderBy(PlaylistTrackTable.indexOnPlaylist to SortOrder.ASC)
                .toList()
        }
    }
}
