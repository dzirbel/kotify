package com.dzirbel.kotify.properties

import com.dzirbel.kotify.network.model.Artist
import com.dzirbel.kotify.network.model.FullArtist
import com.google.common.truth.Truth.assertThat

data class ArtistProperties(
    override val id: String,
    override val name: String,
    val albums: List<AlbumProperties>,
    val genres: List<String> = emptyList()
) : ObjectProperties(type = "artist") {
    fun check(artist: Artist) {
        super.check(artist)

        if (artist is FullArtist) {
            assertThat(artist.followers.total).isAtLeast(0)
            assertThat(artist.genres).containsAtLeastElementsIn(genres)
            assertThat(artist.images).isNotEmpty()
            assertThat(artist.popularity).isIn(0..100)
        }
    }
}
