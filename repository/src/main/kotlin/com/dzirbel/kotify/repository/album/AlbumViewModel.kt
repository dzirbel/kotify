package com.dzirbel.kotify.repository.album

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.AlbumType
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbumTable
import com.dzirbel.kotify.db.util.entitiesFor
import com.dzirbel.kotify.repository.EntityImageViewModel
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.ImageViewModel
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.repository.util.ReleaseDate
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

@Stable
data class AlbumViewModel(
    override val id: String,
    override val name: String,
    override val uri: String? = null,
    override val updatedTime: Instant = CurrentTime.instant,
    override val fullUpdatedTime: Instant? = null,
    val totalTracks: Int? = null,
    val albumType: AlbumType? = null,
    val releaseDate: String? = null,
    val artists: StateFlow<List<ArtistViewModel>?> = LazyTransactionStateFlow("album $id artists") {
        Artist.entitiesFor(id, ArtistAlbumTable.album).map(::ArtistViewModel)
    },
    val images: ImageViewModel = EntityImageViewModel(id, AlbumTable.AlbumImageTable.album),
) : EntityViewModel, ImageViewModel by images {

    val parsedReleaseDate: ReleaseDate? by lazy {
        releaseDate?.let { ReleaseDate.parse(it) }
    }

    constructor(album: Album) : this(
        id = album.id.value,
        uri = album.uri,
        name = album.name,
        updatedTime = album.updatedTime,
        fullUpdatedTime = album.fullUpdatedTime,
        totalTracks = album.totalTracks,
        albumType = album.albumType,
        releaseDate = album.releaseDate,
    )
}
