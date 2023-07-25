package com.dzirbel.kotify.ui.properties

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumType
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.repository.rating.AverageRating
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.ui.components.adapter.properties.PropertyByString
import com.dzirbel.kotify.ui.theme.Dimens
import kotlinx.coroutines.flow.StateFlow

open class AlbumNameProperty<A>(private val toAlbum: (A) -> Album) : PropertyByString<A>(title = "Name") {
    override fun toString(item: A) = toAlbum(item).name

    companion object : AlbumNameProperty<Album>(toAlbum = { it })
    object ForArtistAlbum : AlbumNameProperty<ArtistAlbum>(toAlbum = { it.album.cached })
}

open class AlbumReleaseDateProperty<A>(
    private val toAlbum: (A) -> Album,
) : PropertyByReleaseDate<A>(title = "Release date") {
    override fun releaseDateOf(item: A) = toAlbum(item).parsedReleaseDate

    companion object : AlbumReleaseDateProperty<Album>(toAlbum = { it })
    object ForArtistAlbum : AlbumReleaseDateProperty<ArtistAlbum>(toAlbum = { it.album.cached })
}

open class AlbumTypeDividableProperty<A>(private val toAlbumType: (A) -> AlbumType?) : DividableProperty<A> {
    override val title = "Album type"

    override fun divisionFor(element: A): AlbumType? = toAlbumType(element)

    @Composable
    override fun DivisionIcon(division: Any?) {
        (division as? AlbumType)?.iconName?.let { iconName ->
            CachedIcon(name = iconName, size = Dimens.iconSizeFor(fontSize = MaterialTheme.typography.h5.fontSize))
        }
    }

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compareNullable(first as? AlbumType, second as? AlbumType)
    }

    override fun divisionTitle(division: Any?): String? {
        return (division as? AlbumType)?.displayName ?: "Unknown"
    }

    companion object : AlbumTypeDividableProperty<Album>(toAlbumType = { it.albumType })
    object ForArtistAlbum : AlbumTypeDividableProperty<ArtistAlbum>(toAlbumType = { it.albumGroup })
}

class AlbumRatingProperty(ratings: Map<String, StateFlow<AverageRating>>) : PropertyByAverageRating<Album>(ratings) {
    override fun idOf(element: Album) = element.id.value

    class ForArtistAlbum(ratings: Map<String, StateFlow<AverageRating>>) :
        PropertyByAverageRating<ArtistAlbum>(ratings) {
        override fun idOf(element: ArtistAlbum) = element.albumId.value
    }
}
