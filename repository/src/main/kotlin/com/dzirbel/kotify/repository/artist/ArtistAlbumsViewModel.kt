package com.dzirbel.kotify.repository.artist

import java.time.Instant

data class ArtistAlbumsViewModel(
    val artistAlbums: List<ArtistAlbumViewModel>,
    val updateTime: Instant,
)
