package com.dominiczirbel.network

import com.dominiczirbel.Fixtures
import com.dominiczirbel.PlaylistProperties
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class SpotifyPlaylistsTest {
    @Test
    fun getPlaylists() {
        val playlists = runBlocking { Spotify.Playlists.getPlaylists() }
        assertThat(playlists.items).isNotEmpty()
        Fixtures.playlists.forEach { playlistProperties ->
            val playlist = playlists.items.find { it.id == playlistProperties.id }
            assertThat(playlist).isNotNull()
            playlistProperties.check(playlist!!)
        }
    }

    @Test
    fun getPlaylistsByUser() {
        val playlists = runBlocking { Spotify.Playlists.getPlaylists(userId = Fixtures.userId) }
        assertThat(playlists.items).isNotEmpty()
        Fixtures.playlists.forEach { playlistProperties ->
            val playlist = playlists.items.find { it.id == playlistProperties.id }
            assertThat(playlist).isNotNull()
            playlistProperties.check(playlist!!)
        }
    }

    @Test
    fun getPlaylistCoverImages() {
        Fixtures.playlists.forEach { playlist ->
            val images = runBlocking { Spotify.Playlists.getPlaylistCoverImages(playlistId = playlist.id) }
            assertThat(images).isNotEmpty()
            images.forEach { image ->
                assertThat(image.url).isNotEmpty()
                assertThat(image.width).isGreaterThan(0)
                assertThat(image.height).isGreaterThan(0)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("playlists")
    fun getPlaylist(playlistProperties: PlaylistProperties) {
        val playlist = runBlocking { Spotify.Playlists.getPlaylist(playlistId = playlistProperties.id) }
        playlistProperties.check(playlist)
    }

    @ParameterizedTest
    @MethodSource("playlists")
    fun getPlaylistTracks(playlistProperties: PlaylistProperties) {
        val tracks = runBlocking { Spotify.Playlists.getPlaylistTracks(playlistId = playlistProperties.id) }
        playlistProperties.tracks?.let {
            tracks.items.zip(playlistProperties.tracks).forEach { (playlistTrack, trackProperties) ->
                trackProperties.check(playlistTrack)
            }
        }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun playlists() = Fixtures.playlists
    }
}
