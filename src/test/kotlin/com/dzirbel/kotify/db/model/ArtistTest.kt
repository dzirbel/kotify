package com.dzirbel.kotify.db.model

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.dzirbel.kotify.DatabaseExtension
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyFollowers
import com.dzirbel.kotify.network.model.SpotifyImage
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(DatabaseExtension::class)
internal class ArtistTest {
    @Test
    fun testFromSimplified() {
        val simplifiedSpotifyArtist = SimplifiedSpotifyArtist(
            externalUrls = SpotifyExternalUrl(),
            id = "id1",
            name = "test artist",
            type = "artist",
        )

        val artist = transaction { Artist.from(simplifiedSpotifyArtist) }

        requireNotNull(artist)
        assertThat(artist.id.value).isEqualTo(simplifiedSpotifyArtist.id)
        assertThat(artist.name).isEqualTo(simplifiedSpotifyArtist.name)

        assertThat(artist.popularity).isNull()
        assertThat(artist.followersTotal).isNull()
        assertThat(transaction { artist.images.live }).isEmpty()
        assertThat(transaction { artist.genres.live }).isEmpty()
    }

    @Test
    fun testFromFull() {
        val fullSpotifyArtist = FullSpotifyArtist(
            externalUrls = SpotifyExternalUrl(),
            id = "id1",
            href = "",
            uri = "",
            name = "test artist",
            type = "artist",
            followers = SpotifyFollowers(total = 3),
            genres = listOf("genre 1", "genre 2"),
            images = listOf(
                SpotifyImage(
                    url = "url 1",
                    width = 10,
                    height = 20,
                ),
                SpotifyImage(
                    url = "url 2",
                    width = 15,
                    height = 25,
                ),
            ),
            popularity = 4,
        )

        val artist = transaction { Artist.from(fullSpotifyArtist) }

        requireNotNull(artist)
        assertThat(artist.id.value).isEqualTo(fullSpotifyArtist.id)
        assertThat(artist.name).isEqualTo(fullSpotifyArtist.name)

        assertThat(artist.popularity).isEqualTo(fullSpotifyArtist.popularity)
        assertThat(artist.followersTotal).isEqualTo(fullSpotifyArtist.followers.total)

        assertThat(transaction { artist.images.live.map { it.url } }).containsExactlyInAnyOrder("url 1", "url 2")
        assertThat(transaction { artist.genres.live.map { it.name } }).containsExactlyInAnyOrder("genre 1", "genre 2")
    }
}
