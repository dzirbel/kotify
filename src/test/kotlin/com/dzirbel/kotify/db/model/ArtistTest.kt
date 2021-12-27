package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyFollowers
import com.dzirbel.kotify.network.model.SpotifyImage
import com.google.common.truth.Truth.assertThat
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ArtistTest {
    @BeforeEach
    fun setup() {
        KotifyDatabase.db
    }

    @AfterEach
    fun cleanup() {
        KotifyDatabase.clear()
    }

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
        assertThat(transaction { artist.images.toList() }).isEmpty()
        assertThat(transaction { artist.genres.toList() }).isEmpty()
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

        assertThat(artist.popularity).isEqualTo(fullSpotifyArtist.popularity.toUInt())
        assertThat(artist.followersTotal).isEqualTo(fullSpotifyArtist.followers.total.toUInt())

        assertThat(transaction { artist.images.toList().map { it.url } }).containsExactly("url 1", "url 2")
        assertThat(transaction { artist.genres.toList().map { it.name } }).containsExactly("genre 1", "genre 2")
    }
}
