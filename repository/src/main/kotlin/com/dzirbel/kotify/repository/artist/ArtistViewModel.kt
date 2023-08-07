package com.dzirbel.kotify.repository.artist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.util.largest
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.genre.GenreViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow

@Stable
class ArtistViewModel(artist: Artist) : EntityViewModel(artist) {
    val popularity: Int? = artist.popularity

    val largestImageUrl = LazyTransactionStateFlow("artist $id largest image") { artist.images.largest()?.url }

    val genres = LazyTransactionStateFlow("artist $id genres") { artist.genres.map(::GenreViewModel) }
}
