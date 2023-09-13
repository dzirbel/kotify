package com.dzirbel.kotify.repository.track

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackTable
import com.dzirbel.kotify.db.util.entitiesFor
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.album.AlbumViewModel
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.util.CurrentTime
import java.time.Instant

@Stable
data class TrackViewModel(
    override val id: String,
    override val name: String,
    override val uri: String? = null,
    override val updatedTime: Instant = CurrentTime.instant,
    override val fullUpdatedTime: Instant? = null,
    val trackNumber: Int,
    val durationMs: Long,
    val albumId: String? = null,
    val popularity: Int? = null,
    val album: LazyTransactionStateFlow<AlbumViewModel> = LazyTransactionStateFlow("track $id album") {
        albumId?.let { Album.findById(it) }?.let(::AlbumViewModel)
    },
    val artists: LazyTransactionStateFlow<List<ArtistViewModel>> = LazyTransactionStateFlow("track $id artists") {
        Artist.entitiesFor(id, TrackTable.TrackArtistTable.track).map(::ArtistViewModel)
    },
) : EntityViewModel {

    constructor(track: Track) : this(
        id = track.id.value,
        uri = track.uri,
        name = track.name,
        updatedTime = track.updatedTime,
        fullUpdatedTime = track.fullUpdatedTime,
        trackNumber = track.trackNumber,
        durationMs = track.durationMs,
        albumId = track.albumId?.value,
        popularity = track.popularity,
    )
}
