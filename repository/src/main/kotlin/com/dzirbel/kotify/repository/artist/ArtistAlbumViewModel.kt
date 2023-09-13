package com.dzirbel.kotify.repository.artist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.AlbumType
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.repository.album.AlbumViewModel

@Stable
data class ArtistAlbumViewModel(
    val album: AlbumViewModel,
    val artist: ArtistViewModel,
    val albumGroup: AlbumType? = null,
) {
    constructor(artistAlbum: ArtistAlbum) : this(
        album = AlbumViewModel(artistAlbum.album),
        artist = ArtistViewModel(artistAlbum.artist),
        albumGroup = artistAlbum.albumGroup,
    )
}
