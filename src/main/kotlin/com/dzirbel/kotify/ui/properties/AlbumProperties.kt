package com.dzirbel.kotify.ui.properties

import androidx.compose.runtime.State
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.repository.Rating

object AlbumNameProperty : PropertyByString<Album>(title = "Name") {
    override fun toString(item: Album) = item.name
}

object AlbumReleaseDateProperty : PropertyByReleaseDate<Album>(title = "Release date") {
    override fun releaseDateOf(item: Album) = item.parsedReleaseDate
}

class AlbumRatingProperty(ratings: Map<String, List<State<Rating?>>?>) : PropertyByAverageRating<Album>(ratings) {
    override fun idOf(element: Album) = element.id.value
}
