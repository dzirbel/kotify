package com.dzirbel.kotify.repository.artist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.repository.EntityImageViewModel
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.ImageViewModel
import com.dzirbel.kotify.repository.genre.GenreViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow

@Stable
class ArtistViewModel(artist: Artist) :
    EntityViewModel(artist),
    ImageViewModel by EntityImageViewModel(artist, ArtistTable, ArtistTable.ArtistImageTable.artist) {

    val popularity: Int? = artist.popularity

    val genres = LazyTransactionStateFlow("artist $id genres") { artist.genres.map(::GenreViewModel) }
}
