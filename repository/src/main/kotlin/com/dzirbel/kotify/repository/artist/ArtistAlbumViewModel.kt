package com.dzirbel.kotify.repository.artist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.AlbumType
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.repository.album.AlbumViewModel

@Stable
class ArtistAlbumViewModel(artistAlbum: ArtistAlbum) {
    val albumGroup: AlbumType? = artistAlbum.albumGroup
    val album: AlbumViewModel = AlbumViewModel(artistAlbum.album)
    val artist: ArtistViewModel = ArtistViewModel(artistAlbum.artist)
}
