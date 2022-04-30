package com.dzirbel.kotify.properties

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEmpty
import com.dzirbel.kotify.Fixtures
import com.dzirbel.kotify.containsAllElementsOf
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyArtist

data class ArtistProperties(
    override val id: String,
    override val name: String,
    val albums: List<AlbumProperties>,
    val genres: List<String> = emptyList(),
) : ObjectProperties(type = "artist") {
    fun check(artist: SpotifyArtist) {
        super.check(artist)

        if (artist is FullSpotifyArtist) {
            assertThat(artist.followers.total).isGreaterThanOrEqualTo(0)
            assertThat(artist.genres).containsAllElementsOf(genres)
            assertThat(artist.images).isNotEmpty()
            assertThat(artist.popularity).isBetween(0, Fixtures.MAX_POPULARITY)
        }
    }
}
