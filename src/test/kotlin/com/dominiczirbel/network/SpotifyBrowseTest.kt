package com.dominiczirbel.network

import com.dominiczirbel.Fixtures
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class SpotifyBrowseTest {
    @Test
    fun categories() {
        val categories = runBlocking { Spotify.Browse.getCategories(country = "US") }.items
        assertThat(categories.isNotEmpty())

        // We want to check some real categories, but they might be too unstable to store in Fixtures, so just use the
        // live results. Ideally these might be their own tests, but getting them to run after the getCategories call
        // returns is nontrivial.
        categories.forEach { category ->
            val returnedCategory = runBlocking { Spotify.Browse.getCategory(categoryId = category.id, country = "US") }
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
                seedArtists = Fixtures.artists.map { it.id }.take(5),
                seedGenres = emptyList(),
                seedTracks = emptyList()
            )
        }
        assertThat(recommendations.seeds.isNotEmpty())
        assertThat(recommendations.tracks.isNotEmpty())
    }

    @Test
    fun getRecommendationGenres() {
        val recommendations = runBlocking { Spotify.Browse.getRecommendationGenres() }
        assertThat(recommendations.isNotEmpty())
        assertThat(recommendations).containsAtLeastElementsIn(Fixtures.recommendationGenres)
    }
}
