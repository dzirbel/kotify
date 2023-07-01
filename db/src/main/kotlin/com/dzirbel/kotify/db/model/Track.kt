package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.ReadWriteCachedProperty
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.cached
import com.dzirbel.kotify.db.cachedAsList
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyTrack
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.time.Instant

object TrackTable : SpotifyEntityTable(name = "tracks") {
    val discNumber: Column<Int> = integer("disc_number")
    val durationMs: Column<Long> = long("duration_ms")
    val explicit: Column<Boolean> = bool("explicit")
    val local: Column<Boolean> = bool("local")
    val playable: Column<Boolean?> = bool("playable").nullable()
    val trackNumber: Column<Int> = integer("track_number")
    val popularity: Column<Int?> = integer("popularity").nullable()

    val album: Column<EntityID<String>?> = reference("album", AlbumTable).nullable()

    object TrackArtistTable : Table() {
        val track = reference("track", TrackTable)
        val artist = reference("artist", ArtistTable)
        override val primaryKey = PrimaryKey(track, artist)
    }

    object SavedTracksTable : SavedEntityTable(name = "saved_tracks")
}

class Track(id: EntityID<String>) : SpotifyEntity(id = id, table = TrackTable) {
    var discNumber: Int by TrackTable.discNumber
    var durationMs: Long by TrackTable.durationMs
    var explicit: Boolean by TrackTable.explicit
    var local: Boolean by TrackTable.local
    var playable: Boolean? by TrackTable.playable
    var trackNumber: Int by TrackTable.trackNumber
    var popularity: Int? by TrackTable.popularity

    val album: ReadWriteCachedProperty<Album?> by (Album optionalReferencedOn TrackTable.album).cached()

    val artists: ReadWriteCachedProperty<List<Artist>> by (Artist via TrackTable.TrackArtistTable).cachedAsList()

    companion object : SpotifyEntityClass<Track, SpotifyTrack>(TrackTable) {
        override fun Track.update(networkModel: SpotifyTrack) {
            discNumber = networkModel.discNumber
            durationMs = networkModel.durationMs
            explicit = networkModel.explicit
            local = networkModel.isLocal
            playable = networkModel.isPlayable
            trackNumber = networkModel.trackNumber
            networkModel.album
                ?.let { Album.from(it) }
                ?.let { album.set(it) }

            artists.set(networkModel.artists.mapNotNull { Artist.from(it) })

            if (networkModel is SimplifiedSpotifyTrack) {
                networkModel.popularity?.let {
                    popularity = it
                }
            }

            if (networkModel is FullSpotifyTrack) {
                fullUpdatedTime = Instant.now()
                popularity = networkModel.popularity
            }
        }
    }
}
