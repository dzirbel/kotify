package com.dominiczirbel.network

import com.dominiczirbel.Fixtures
import com.dominiczirbel.TAG_NETWORK
import com.dominiczirbel.network.model.SimplifiedPlaylist
import com.dominiczirbel.properties.PlaylistProperties
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path

@Tag(TAG_NETWORK)
class SpotifyPlaylistsTest {
    @Test
    fun getPlaylists() {
        val playlists = runBlocking { Spotify.Playlists.getPlaylists().fetchAll<SimplifiedPlaylist>() }

        // don't zip since there are playlists that aren't in Fixtures
        Fixtures.playlists.forEach { playlistProperties ->
            val playlist = playlists.first { it.id == playlistProperties.id }
            playlistProperties.check(playlist)
        }
    }

    @Test
    fun getPlaylistsByUser() {
        val playlists = runBlocking {
            Spotify.Playlists.getPlaylists(userId = Fixtures.userId).fetchAll<SimplifiedPlaylist>()
        }

        // don't zip since there are playlists that aren't in Fixtures
        Fixtures.playlists.forEach { playlistProperties ->
            val playlist = playlists.first { it.id == playlistProperties.id }
            playlistProperties.check(playlist)
        }
    }

    @Test
    fun createAndEditPlaylist() {
        val name = "Test Playlist ${System.currentTimeMillis()}"
        val playlist = runBlocking {
            Spotify.Playlists.createPlaylist(userId = Fixtures.userId, name = name, public = false)
        }

        assertThat(playlist.id).isNotEmpty()
        assertThat(playlist.name).isEqualTo(name)
        assertThat(playlist.public).isEqualTo(false)
        assertThat(playlist.owner.id).isEqualTo(Fixtures.userId)

        val updatedName = "Test Playlist v2 ${System.currentTimeMillis()}"
        val updatedDescription = "test description"
        runBlocking {
            Spotify.Playlists.changePlaylistDetails(
                playlistId = playlist.id,
                name = updatedName,
                description = updatedDescription
            )
        }

        val updatedPlaylist = runBlocking { Spotify.Playlists.getPlaylist(playlistId = playlist.id) }
        assertThat(updatedPlaylist.id).isEqualTo(playlist.id)
        assertThat(updatedPlaylist.name).isEqualTo(updatedName)
        assertThat(updatedPlaylist.description).isEqualTo(updatedDescription)

        runBlocking {
            Spotify.Playlists.addItemsToPlaylist(
                playlistId = playlist.id,
                uris = Fixtures.tracks.map { "spotify:track:${it.id}" }
            )
        }

        val playlistWithTracks = runBlocking { Spotify.Playlists.getPlaylist(playlistId = playlist.id) }
        assertThat(playlistWithTracks.id).isEqualTo(playlist.id)
        assertThat(playlistWithTracks.tracks.total).isEqualTo(Fixtures.tracks.size)
        assertThat(playlistWithTracks.tracks.items.size).isEqualTo(Fixtures.tracks.size)
        playlistWithTracks.tracks.items.zip(Fixtures.tracks).forEach { (track, trackProperties) ->
            trackProperties.check(track)
        }

        runBlocking {
            // A, B, C, D, E, ... -> A, D, B, C, E, ...
            Spotify.Playlists.reorderPlaylistItems(
                playlistId = playlist.id,
                rangeStart = 1,
                rangeLength = 2,
                insertBefore = 4
            )
        }

        val reorderedPlaylist = runBlocking { Spotify.Playlists.getPlaylist(playlistId = playlist.id) }
        assertThat(reorderedPlaylist.id).isEqualTo(playlist.id)
        assertThat(reorderedPlaylist.tracks.total).isEqualTo(Fixtures.tracks.size)
        assertThat(reorderedPlaylist.tracks.items.size).isEqualTo(Fixtures.tracks.size)
        val reorderedTracks = Fixtures.tracks.toMutableList()
        // B
        val track1 = reorderedTracks.removeAt(1)
        // C
        val track2 = reorderedTracks.removeAt(1)
        reorderedTracks.add(2, track1)
        reorderedTracks.add(3, track2)
        reorderedPlaylist.tracks.items.zip(reorderedTracks).forEach { (track, trackProperties) ->
            trackProperties.check(track)
        }

        val replaceTracks = Fixtures.albums.values.first().take(3)
        runBlocking {
            Spotify.Playlists.replacePlaylistItems(
                playlistId = playlist.id,
                uris = replaceTracks.map { "spotify:track:${it.id}" }
            )
        }

        val replacedPlaylist = runBlocking { Spotify.Playlists.getPlaylist(playlistId = playlist.id) }
        assertThat(replacedPlaylist.id).isEqualTo(playlist.id)
        assertThat(replacedPlaylist.tracks.total).isEqualTo(replaceTracks.size)
        assertThat(replacedPlaylist.tracks.items.size).isEqualTo(replaceTracks.size)
        replacedPlaylist.tracks.items.zip(replaceTracks).forEach { (track, trackProperties) ->
            trackProperties.check(track)
        }

        runBlocking {
            Spotify.Playlists.removePlaylistTracks(
                playlistId = playlist.id,
                tracks = replaceTracks.take(2).map { "spotify:track:${it.id}" }
            )
        }

        val removedPlaylist = runBlocking { Spotify.Playlists.getPlaylist(playlistId = playlist.id) }
        assertThat(removedPlaylist.id).isEqualTo(playlist.id)
        assertThat(removedPlaylist.tracks.total).isEqualTo(1)
        assertThat(removedPlaylist.tracks.items.size).isEqualTo(1)
        removedPlaylist.tracks.items.zip(replaceTracks.takeLast(1)).forEach { (track, trackProperties) ->
            trackProperties.check(track)
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

    @Test
    fun uploadPlaylistCoverImage() {
        val name = "Custom Image Playlist ${System.currentTimeMillis()}"
        val playlist = runBlocking {
            Spotify.Playlists.createPlaylist(userId = Fixtures.userId, name = name, public = false)
        }

        val bytes = Files.readAllBytes(Path.of("src/test/resources/test-image.jpg"))

        runBlocking { Spotify.Playlists.uploadPlaylistCoverImage(playlistId = playlist.id, jpegImage = bytes) }

        val images = runBlocking { Spotify.Playlists.getPlaylistCoverImages(playlistId = playlist.id) }
        assertThat(images).hasSize(1)
        val image = images.first()

        val client = OkHttpClient.Builder().build()
        val request = Request.Builder().url(image.url).build()
        val response = client.newCall(request).execute()

        // note: the downloaded image is not identical to the uploaded one due to resizing
        assertThat(response.headers["Content-Type"]).isEqualTo("image/jpeg")
        assertThat(response.body?.bytes()).isNotEmpty()
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
            tracks.items.zip(it).forEach { (playlistTrack, trackProperties) ->
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
