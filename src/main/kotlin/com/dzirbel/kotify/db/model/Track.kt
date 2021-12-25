package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.Repository
import com.dzirbel.kotify.db.SpotifyEntity
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyTrack
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column

object TrackTable : SpotifyEntityTable() {
    val durationMs: Column<ULong> = ulong("duration_ms")
}

class Track(id: EntityID<String>) : SpotifyEntity(id = id, table = TrackTable) {
    var durationMs: ULong by TrackTable.durationMs

    companion object : SpotifyEntityClass<Track, SpotifyTrack>(TrackTable) {
        override fun Track.update(networkModel: SpotifyTrack) {
            durationMs = networkModel.durationMs.toULong() // TODO use ULong in network model?
        }
    }
}

object TrackRepository : Repository<Track, SpotifyTrack>(Track) {
    override suspend fun fetch(id: String) = Spotify.Tracks.getTrack(id = id)
    override suspend fun fetch(ids: List<String>) = Spotify.Tracks.getTracks(ids = ids)
}
