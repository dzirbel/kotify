package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.Episode
import com.dzirbel.kotify.db.EpisodeTable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and

object PlaylistTrackTable : IntIdTable() {
    val addedAd: Column<String?> = varchar("added_at", 20).nullable()
    val isLocal: Column<Boolean> = bool("is_local")
    val indexOnPlaylist: Column<Int> = integer("index_on_playlist")

    val addedBy: Column<EntityID<String>> = reference("user", UserTable)
    val playlist: Column<EntityID<String>> = reference("playlist", PlaylistTable)
    val track: Column<EntityID<String>?> = reference("track", TrackTable).nullable()
    val episode: Column<EntityID<String>?> = reference("episode", EpisodeTable).nullable()

    init {
        uniqueIndex(playlist, track)
        uniqueIndex(playlist, episode)
    }
}

class PlaylistTrack(id: EntityID<Int>) : IntEntity(id) {
    var playlistId: EntityID<String> by PlaylistTrackTable.playlist
    var trackId: EntityID<String>? by PlaylistTrackTable.track
    var episodeId: EntityID<String>? by PlaylistTrackTable.episode

    var addedAt: String? by PlaylistTrackTable.addedAd
    var isLocal: Boolean by PlaylistTrackTable.isLocal
    var indexOnPlaylist: Int by PlaylistTrackTable.indexOnPlaylist

    var addedBy: User by User referencedOn PlaylistTrackTable.addedBy
    var playlist: Playlist by Playlist referencedOn PlaylistTrackTable.playlist
    var track: Track? by Track optionalReferencedOn PlaylistTrackTable.track
    var episode: Episode? by Episode optionalReferencedOn PlaylistTrackTable.episode

    companion object : IntEntityClass<PlaylistTrack>(PlaylistTrackTable) {
        fun findOrCreateFromTrack(trackId: String, playlistId: String): PlaylistTrack {
            return find { (PlaylistTrackTable.track eq trackId) and (PlaylistTrackTable.playlist eq playlistId) }
                .firstOrNull()
                ?: new {
                    this.trackId = EntityID(id = trackId, table = TrackTable)
                    this.playlistId = EntityID(id = playlistId, table = PlaylistTable)
                }
        }

        fun findOrCreateFromEpisode(episodeId: String, playlistId: String): PlaylistTrack {
            return find { (PlaylistTrackTable.episode eq episodeId) and (PlaylistTrackTable.playlist eq playlistId) }
                .firstOrNull()
                ?: new {
                    this.episodeId = EntityID(id = episodeId, table = EpisodeTable)
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
