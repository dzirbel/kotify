package com.dzirbel.kotify.repository.artist

import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.util.LazyTransactionStateFlow
import com.dzirbel.kotify.db.util.largest
import com.dzirbel.kotify.repository.genre.GenreViewModel

class ArtistViewModel(artist: Artist) {
    val id: String = artist.id.value
    val uri: String? = artist.uri
    val name: String = artist.name
    val popularity: Int? = artist.popularity

    val largestImageUrl = LazyTransactionStateFlow("artist $id largest image") { artist.images.largest()?.url }

    val genres = LazyTransactionStateFlow("artist $id genres") { artist.genres.map(::GenreViewModel) }
}
