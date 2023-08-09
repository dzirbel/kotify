package com.dzirbel.kotify.repository.album

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumTable
import com.dzirbel.kotify.db.model.AlbumType
import com.dzirbel.kotify.repository.EntityImageViewModel
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.ImageViewModel
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.repository.util.ReleaseDate

@Stable
class AlbumViewModel(album: Album) :
    EntityViewModel(album),
    ImageViewModel by EntityImageViewModel(album, AlbumTable, AlbumTable.AlbumImageTable.album) {

    val totalTracks: Int? = album.totalTracks
    val albumType: AlbumType? = album.albumType

    val releaseDate: String? = album.releaseDate
    val parsedReleaseDate: ReleaseDate? by lazy {
        releaseDate?.let { ReleaseDate.parse(it) }
    }

    val artists = LazyTransactionStateFlow("album $id artists") { album.artists.map(::ArtistViewModel) }
}
