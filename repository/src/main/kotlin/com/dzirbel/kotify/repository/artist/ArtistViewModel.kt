package com.dzirbel.kotify.repository.artist

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistTable
import com.dzirbel.kotify.db.model.Genre
import com.dzirbel.kotify.db.util.entitiesFor
import com.dzirbel.kotify.repository.EntityImageViewModel
import com.dzirbel.kotify.repository.EntityViewModel
import com.dzirbel.kotify.repository.ImageViewModel
import com.dzirbel.kotify.repository.genre.GenreViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

@Stable
data class ArtistViewModel(
    override val id: String,
    override val name: String,
    override val uri: String? = null,
    override val updatedTime: Instant = CurrentTime.instant,
    override val fullUpdatedTime: Instant? = null,
    val popularity: Int? = null,
    val genres: StateFlow<List<GenreViewModel>?> = LazyTransactionStateFlow("artist $id genres") {
        Genre.entitiesFor(id, ArtistTable.ArtistGenreTable.artist).map(::GenreViewModel)
    },
    val images: ImageViewModel = EntityImageViewModel(id, ArtistTable.ArtistImageTable.artist),
) : EntityViewModel, ImageViewModel by images {

    constructor(artist: Artist) : this(
        id = artist.id.value,
        uri = artist.uri,
        name = artist.name,
        updatedTime = artist.updatedTime,
        fullUpdatedTime = artist.fullUpdatedTime,
        popularity = artist.popularity,
    )
}
