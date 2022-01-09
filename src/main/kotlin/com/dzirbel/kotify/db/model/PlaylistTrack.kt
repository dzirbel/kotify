package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.ReadWriteCachedProperty
import com.dzirbel.kotify.db.cached
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere

object PlaylistTrackTable : IntIdTable() {
    val addedAd: Column<String?> = varchar("added_at", 20).nullable()
    val isLocal: Column<Boolean> = bool("is_local")

    val addedBy: Column<EntityID<String>> = reference("user", UserTable)
    val playlist: Column<EntityID<String>> = reference("playlist", PlaylistTable)
    val track: Column<EntityID<String>> = reference("track", TrackTable)

    init {
        uniqueIndex(playlist, track)
    }
}

class PlaylistTrack(id: EntityID<Int>) : IntEntity(id) {
    var addedAt: String? by PlaylistTrackTable.addedAd
    var isLocal: Boolean by PlaylistTrackTable.isLocal

    val addedBy: ReadWriteCachedProperty<User> by (User referencedOn PlaylistTrackTable.addedBy).cached()
    val playlist: ReadWriteCachedProperty<Playlist> by (Playlist referencedOn PlaylistTrackTable.playlist).cached()
    val track: ReadWriteCachedProperty<Track> by (Track referencedOn PlaylistTrackTable.track).cached()

    companion object : IntEntityClass<PlaylistTrack>(PlaylistTrackTable) {
        /**
         * Returns the [PlaylistTrack] for the given [track] and [playlist], creating one if it does not exist.
         */
        private fun recordFor(track: Track, playlist: Playlist): PlaylistTrack {
            return find {
                @Suppress("UnnecessaryParentheses")
                (PlaylistTrackTable.track eq track.id) and (PlaylistTrackTable.playlist eq playlist.id)
            }
                .firstOrNull()
                ?: new {
                    this.track.set(track)
                    this.playlist.set(playlist)
                }
        }

        fun from(spotifyPlaylistTrack: SpotifyPlaylistTrack, playlist: Playlist): PlaylistTrack? {
            return Track.from(spotifyPlaylistTrack.track)?.let { track ->
                recordFor(track = track, playlist = playlist).apply {
                    User.from(spotifyPlaylistTrack.addedBy)?.let { addedBy.set(it) }
                    spotifyPlaylistTrack.addedAt?.let { addedAt = it }
                    isLocal = spotifyPlaylistTrack.isLocal
                }
            }
        }

        // TODO move to repository?
        fun invalidate(playlistId: String) {
            PlaylistTrackTable.deleteWhere { PlaylistTrackTable.playlist eq playlistId }
        }
    }
}
