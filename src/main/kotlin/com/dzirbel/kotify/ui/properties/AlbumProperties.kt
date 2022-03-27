package com.dzirbel.kotify.ui.properties

import androidx.compose.runtime.State
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.compareNullable
import com.dzirbel.kotify.util.capitalize

object AlbumNameProperty : PropertyByString<Album>(title = "Name") {
    override fun toString(item: Album) = item.name
}

object AlbumReleaseDateProperty : PropertyByReleaseDate<Album>(title = "Release date") {
    override fun releaseDateOf(item: Album) = item.parsedReleaseDate
}

object AlbumTypeDividableProperty : DividableProperty<Album> {
    override val title = "Album type"

    override fun divisionFor(element: Album): SpotifyAlbum.Type? = element.albumType

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compareNullable(first as? SpotifyAlbum.Type, second as? SpotifyAlbum.Type)
    }

    override fun divisionTitle(division: Any?): String? {
        return (division as? SpotifyAlbum.Type)?.name?.lowercase()?.capitalize()
    }
}

class AlbumRatingProperty(ratings: Map<String, List<State<Rating?>>?>) : PropertyByAverageRating<Album>(ratings) {
    override fun idOf(element: Album) = element.id.value
}
