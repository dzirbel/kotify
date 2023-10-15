package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.network.properties.PlaylistProperties
import com.dzirbel.kotify.util.retryForResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Tag(TAG_NETWORK)
@ExtendWith(NetworkExtension::class)
class SpotifyPlaylistsTest {
    @Test
    fun getPlaylists() {
        val playlists = runBlocking { Spotify.Playlists.getPlaylists().asFlow().toList() }

        // don't zip since there are playlists that aren't in Fixtures
        NetworkFixtures.playlists.forEach { playlistProperties ->
            val playlist = playlists.first { it.id == playlistProperties.id }
            playlistProperties.check(playlist)
        }
    }

    @Test
    fun getPlaylistsByUser() {
        val playlists = runBlocking {
            Spotify.Playlists.getPlaylists(userId = NetworkFixtures.userId).asFlow().toList()
        }

        // don't zip since there are playlists that aren't in Fixtures
        NetworkFixtures.playlists.forEach { playlistProperties ->
            val playlist = playlists.first { it.id == playlistProperties.id }
            playlistProperties.check(playlist)
        }
    }

    @Test
    fun createAndEditPlaylist() {
        val name = "Test Playlist ${UUID.randomUUID()}"
        val playlist = runBlocking {
            Spotify.Playlists.createPlaylist(userId = NetworkFixtures.userId, name = name, public = false)
        }

        assertThat(playlist.id).isNotEmpty()
        assertThat(playlist.name).isEqualTo(name)
        assertThat(playlist.public).isNotNull().isFalse()
        assertThat(playlist.owner.id).isEqualTo(NetworkFixtures.userId)

        val updatedName = "$name v2"
        val updatedDescription = "test description"
        runBlocking {
            Spotify.Playlists.changePlaylistDetails(
                playlistId = playlist.id,
                name = updatedName,
                description = updatedDescription,
            )
        }

        retryForResult(attempts = 30, delayBetweenAttempts = 1.seconds) {
            val updatedPlaylist = runBlocking { Spotify.Playlists.getPlaylist(playlistId = playlist.id) }
            assertThat(updatedPlaylist.id).isEqualTo(playlist.id)
            assertThat(updatedPlaylist.name).isEqualTo(updatedName)
            assertThat(updatedPlaylist.description).isEqualTo(updatedDescription)
        }

        runBlocking {
            Spotify.Playlists.addItemsToPlaylist(
                playlistId = playlist.id,
                uris = NetworkFixtures.tracks.map { "spotify:track:${it.id}" },
            )
        }

        val playlistWithTracks = runBlocking { Spotify.Playlists.getPlaylist(playlistId = playlist.id) }
        assertThat(playlistWithTracks.id).isEqualTo(playlist.id)
        assertThat(playlistWithTracks.tracks.total).isEqualTo(NetworkFixtures.tracks.size)
        assertThat(playlistWithTracks.tracks.items.size).isEqualTo(NetworkFixtures.tracks.size)
        playlistWithTracks.tracks.items.zip(NetworkFixtures.tracks).forEach { (track, trackProperties) ->
            trackProperties.check(track)
        }

        runBlocking {
            // A, B, C, D, E, ... -> A, D, B, C, E, ...
            Spotify.Playlists.reorderPlaylistItems(
                playlistId = playlist.id,
                rangeStart = 1,
                rangeLength = 2,
                insertBefore = 4,
            )
        }

        val reorderedPlaylist = runBlocking { Spotify.Playlists.getPlaylist(playlistId = playlist.id) }
        assertThat(reorderedPlaylist.id).isEqualTo(playlist.id)
        assertThat(reorderedPlaylist.tracks.total).isEqualTo(NetworkFixtures.tracks.size)
        assertThat(reorderedPlaylist.tracks.items.size).isEqualTo(NetworkFixtures.tracks.size)
        val reorderedTracks = NetworkFixtures.tracks.toMutableList()
        // B
        val track1 = reorderedTracks.removeAt(1)
        // C
        val track2 = reorderedTracks.removeAt(1)
        reorderedTracks.add(2, track1)
        reorderedTracks.add(3, track2)
        reorderedPlaylist.tracks.items.zip(reorderedTracks).forEach { (track, trackProperties) ->
            trackProperties.check(track)
        }

        val replaceTracks = NetworkFixtures.albums.values.first().take(3)
        runBlocking {
            Spotify.Playlists.replacePlaylistItems(
                playlistId = playlist.id,
                uris = replaceTracks.map { "spotify:track:${it.id}" },
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
                tracks = replaceTracks.take(2).map { "spotify:track:${it.id}" },
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
        NetworkFixtures.playlists.forEach { playlist ->
            val images = runBlocking { Spotify.Playlists.getPlaylistCoverImages(playlistId = playlist.id) }

            assertThat(images).isNotEmpty()
            images.forEach { image ->
                assertThat(image.url).isNotEmpty()
                assertThat(image.width).isNotNull().isGreaterThan(0)
                assertThat(image.height).isNotNull().isGreaterThan(0)
            }
        }
    }

    @Test
    fun uploadPlaylistCoverImage() {
        val name = "Custom Image Playlist ${UUID.randomUUID()}"
        val playlist = runBlocking {
            Spotify.Playlists.createPlaylist(userId = NetworkFixtures.userId, name = name, public = false)
        }

        val bytes = Files.readAllBytes(Path.of("src/test/resources/test-image.jpg"))

        runBlocking { Spotify.Playlists.uploadPlaylistCoverImage(playlistId = playlist.id, jpegImage = bytes) }

        val image = retryForResult(attempts = 3, delayBetweenAttempts = 250.milliseconds) {
            val images = runBlocking { Spotify.Playlists.getPlaylistCoverImages(playlistId = playlist.id) }
            assertThat(images).hasSize(1)
            images.first()
        }

        val client = OkHttpClient.Builder().build()
        val request = Request.Builder().url(image.url).build()
        val response = client.newCall(request).execute()

        // note: the downloaded image is not identical to the uploaded one due to resizing
        assertThat(response.headers["Content-Type"]).isEqualTo("image/jpeg")
        assertThat(response.body?.bytes()).isNotNull().isNotEmpty()
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

        playlistProperties.tracks?.let { trackProperties ->
            tracks.items.zip(trackProperties).forEach { (playlistTrack, trackProperties) ->
                trackProperties.check(playlistTrack)
            }
        }
    }

    companion object {
        @JvmStatic
        fun playlists() = NetworkFixtures.playlists
    }
}
