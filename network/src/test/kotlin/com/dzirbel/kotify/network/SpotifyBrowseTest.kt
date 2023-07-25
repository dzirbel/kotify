package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.dzirbel.kotify.util.containsAllElementsOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag(TAG_NETWORK)
internal class SpotifyBrowseTest {
    @Test
    fun categories() {
        val categories = runBlocking { Spotify.Browse.getCategories(country = "US", locale = "en_US") }

        assertThat(categories.items).isNotEmpty()

        // We want to check some real categories, but they might be too unstable to store in Fixtures, so just use the
        // live results. Ideally these might be their own tests, but getting them to run after the getCategories call
        // returns is nontrivial.
        categories.items.forEach { category ->
            val returnedCategory = runBlocking {
                Spotify.Browse.getCategory(categoryId = category.id, country = "US", locale = "en_US")
            }

            assertThat(returnedCategory.copy(icons = emptyList())).isEqualTo(category.copy(icons = emptyList()))

            val playlists = runBlocking {
                Spotify.Browse.getCategoryPlaylists(categoryId = category.id, country = "US")
            }
            assertThat(playlists.href).isNotEmpty()
            assertThat(playlists.items).isNotNull()
        }
    }

    @Test
    fun getFeaturedPlaylists() {
        val playlists = runBlocking { Spotify.Browse.getFeaturedPlaylists() }

        assertThat(playlists.items).isNotEmpty()
    }

    @Test
    fun getNewReleases() {
        val albums = runBlocking { Spotify.Browse.getNewReleases() }

        assertThat(albums.items).isNotEmpty()
    }

    @Test
    fun getRecommendations() {
        val recommendations = runBlocking {
            Spotify.Browse.getRecommendations(
                seedArtists = NetworkFixtures.artists.map { it.id }.take(5),
                seedGenres = emptyList(),
                seedTracks = emptyList(),
            )
        }

        assertThat(recommendations.seeds).isNotEmpty()
        assertThat(recommendations.tracks).isNotEmpty()
    }

    @Test
    fun getRecommendationGenres() {
        val recommendations = runBlocking { Spotify.Browse.getRecommendationGenres() }

        assertThat(recommendations).isNotEmpty()
        assertThat(recommendations).containsAllElementsOf(NetworkFixtures.recommendationGenres)
    }
}
