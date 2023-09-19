package com.dzirbel.kotify.repository.album

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.repository.CacheStrategy
import com.dzirbel.kotify.repository.ConvertingRepository
import com.dzirbel.kotify.repository.DatabaseRepository
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.convertToDB
import com.dzirbel.kotify.repository.track.TrackRepository
import com.dzirbel.kotify.repository.track.TrackViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.time.Instant

interface AlbumTracksRepository :
    Repository<AlbumTracksViewModel>,
    ConvertingRepository<List<Track>, List<SimplifiedSpotifyTrack>>

class DatabaseAlbumTracksRepository(scope: CoroutineScope, private val trackRepository: TrackRepository) :
    DatabaseRepository<AlbumTracksViewModel, List<Track>, List<SimplifiedSpotifyTrack>>(
        entityName = "album tracks",
        entityNamePlural = "albums tracks",
        scope = scope,
    ),
    AlbumTracksRepository {

    // use long TTLs since album tracks are not expected to change
    override val defaultCacheStrategy = CacheStrategy.TTL<AlbumTracksViewModel>(
        transientTTL = CacheStrategy.TTL.longTransientDuration,
        invalidTTL = CacheStrategy.TTL.longInvalidDuration,
        getUpdateTime = { it.updateTime },
    )

    override suspend fun fetchFromRemote(id: String): List<SimplifiedSpotifyTrack> {
        return Spotify.Albums.getAlbumTracks(id = id).asFlow().toList()
    }

    override fun fetchFromDatabase(id: String): Pair<List<Track>, Instant>? {
        return Album.findById(id)?.let { album ->
            album.tracksFetched?.let { tracksFetched ->
                val tracks = album.tracks.toList().takeIf { it.size == album.totalTracks }
                tracks?.let { Pair(it, tracksFetched) }
            }
        }
    }

    override fun convertToDB(id: String, networkModel: List<SimplifiedSpotifyTrack>, fetchTime: Instant): List<Track> {
        // TODO do not ignore tracks with null id
        val tracks = networkModel
            .mapNotNull { track -> trackRepository.convertToDB(track, fetchTime) }
            .onEach { track -> trackRepository.update(track.id.value, track, fetchTime) }

        AlbumTable.update(where = { AlbumTable.id eq id }) {
            it[tracksFetched] = fetchTime
        }

        val databaseTrackIds = AlbumTable.AlbumTrackTable
            .slice(AlbumTable.AlbumTrackTable.track)
            .select { AlbumTable.AlbumTrackTable.album eq id }
            .mapTo(mutableSetOf()) { row -> row[AlbumTable.AlbumTrackTable.track].value }

        val networkTrackIds = networkModel.mapNotNullTo(mutableSetOf()) { it.id }

        databaseTrackIds.minus(networkTrackIds).takeIf { it.isNotEmpty() }?.let { trackIdsToDelete ->
            AlbumTable.AlbumTrackTable.deleteWhere { (album eq id) and (track inList trackIdsToDelete) }
        }

        networkTrackIds.minus(databaseTrackIds).takeIf { it.isNotEmpty() }?.let { trackIdsToInsert ->
            AlbumTable.AlbumTrackTable.batchInsert(trackIdsToInsert, shouldReturnGeneratedValues = false) { trackId ->
                this[AlbumTable.AlbumTrackTable.album] = id
                this[AlbumTable.AlbumTrackTable.track] = trackId
            }
        }

        return tracks
    }

    override fun convertToVM(databaseModel: List<Track>, fetchTime: Instant): AlbumTracksViewModel {
        return AlbumTracksViewModel(
            tracks = databaseModel.map { TrackViewModel(it) },
            updateTime = fetchTime,
        )
    }
}
