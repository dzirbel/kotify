package com.dzirbel.kotify.ui.properties

import androidx.compose.runtime.State
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.compareNullable

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

open class AlbumTypeDividableProperty<A>(private val toAlbumType: (A) -> SpotifyAlbum.Type?) : DividableProperty<A> {
    override val title = "Album type"

    override fun divisionFor(element: A): SpotifyAlbum.Type? = toAlbumType(element)

    override fun divisionIconName(division: Any?) = (division as? SpotifyAlbum.Type)?.iconName

    override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
        return sortOrder.compareNullable(first as? SpotifyAlbum.Type, second as? SpotifyAlbum.Type)
    }

    override fun divisionTitle(division: Any?): String? {
        return (division as? SpotifyAlbum.Type)?.displayName ?: "Unknown"
    }

    companion object : AlbumTypeDividableProperty<Album>(toAlbumType = { it.albumType })
    object ForArtistAlbum : AlbumTypeDividableProperty<ArtistAlbum>(toAlbumType = { it.albumGroup })
}

class AlbumRatingProperty(ratings: Map<String, List<State<Rating?>>?>) : PropertyByAverageRating<Album>(ratings) {
    override fun idOf(element: Album) = element.id.value

    class ForArtistAlbum(ratings: Map<String, List<State<Rating?>>?>) : PropertyByAverageRating<ArtistAlbum>(ratings) {
        override fun idOf(element: ArtistAlbum) = element.albumId.value
    }
}
