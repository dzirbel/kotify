package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.Repository
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.cachedAsList
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyTrack
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.time.Instant

object TrackTable : SpotifyEntityTable(name = "tracks") {
    val discNumber: Column<UInt> = uinteger("disc_number")
    val durationMs: Column<ULong> = ulong("duration_ms")
    val explicit: Column<Boolean> = bool("explicit")
    val local: Column<Boolean> = bool("local")
    val playable: Column<Boolean?> = bool("playable").nullable()
    val trackNumber: Column<UInt> = uinteger("track_number")
    val popularity: Column<UInt?> = uinteger("popularity").nullable()

    val album: Column<EntityID<String>?> = reference("album", AlbumTable).nullable()

    object TrackArtistTable : Table() {
        val track = reference("track", TrackTable)
        val artist = reference("artist", ArtistTable)
        override val primaryKey = PrimaryKey(track, artist)
    }
}

class Track(id: EntityID<String>) : SpotifyEntity(id = id, table = TrackTable) {
    var discNumber: UInt by TrackTable.discNumber
    var durationMs: ULong by TrackTable.durationMs
    var explicit: Boolean by TrackTable.explicit
    var local: Boolean by TrackTable.local
    var playable: Boolean? by TrackTable.playable
    var trackNumber: UInt by TrackTable.trackNumber
    var popularity: UInt? by TrackTable.popularity

    var album: Album? by Album optionalReferencedOn TrackTable.album

    var artists: List<Artist> by (Artist via TrackTable.TrackArtistTable).cachedAsList()

    companion object : SpotifyEntityClass<Track, SpotifyTrack>(TrackTable) {
        override fun Track.update(networkModel: SpotifyTrack) {
            discNumber = networkModel.discNumber.toUInt()
            durationMs = networkModel.durationMs.toULong() // TODO use ULong in network model?
            explicit = networkModel.explicit
            local = networkModel.isLocal
            playable = networkModel.isPlayable
            trackNumber = networkModel.trackNumber.toUInt()
            networkModel.album?.let {
                album = Album.from(it)
            }

            artists = networkModel.artists.mapNotNull { Artist.from(it) }

            if (networkModel is SimplifiedSpotifyTrack) {
                networkModel.popularity?.let {
                    popularity = it.toUInt()
                }
            }

            if (networkModel is FullSpotifyTrack) {
                fullUpdatedTime = Instant.now()
                popularity = networkModel.popularity.toUInt()
            }
        }
    }
}

object TrackRepository : Repository<Track, SpotifyTrack>(Track) {
    override suspend fun fetch(id: String) = Spotify.Tracks.getTrack(id = id)
    override suspend fun fetch(ids: List<String>) = Spotify.Tracks.getTracks(ids = ids)
}
