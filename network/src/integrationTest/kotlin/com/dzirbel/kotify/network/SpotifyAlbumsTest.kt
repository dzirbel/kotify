package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.network.properties.AlbumProperties
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(NetworkExtension::class)
internal class SpotifyAlbumsTest {
    @ParameterizedTest
    @MethodSource("albums")
    fun getAlbum(albumProperties: AlbumProperties) {
        val album = runBlocking { Spotify.Albums.getAlbum(albumProperties.id) }

        albumProperties.check(album)
    }

    @Test
    fun getAlbumNotFound() {
        val error = runBlocking {
            assertThrows<Spotify.SpotifyError> { Spotify.Albums.getAlbum(NetworkFixtures.notFoundId) }
        }

        assertThat(error.code).isEqualTo(404)
    }

    @Test
    fun getAlbums() {
        val albums = runBlocking { Spotify.Albums.getAlbums(NetworkFixtures.albums.keys.map { it.id }) }

        assertThat(albums.size).isEqualTo(NetworkFixtures.albums.size)
        albums.zip(NetworkFixtures.albums.keys).forEach { (album, albumProperties) -> albumProperties.check(album) }
    }

    @ParameterizedTest
    @MethodSource("albums")
    fun getAlbumTracks(albumProperties: AlbumProperties) {
        val tracks = runBlocking { Spotify.Albums.getAlbumTracks(albumProperties.id).asFlow().toList() }
        val trackProperties = NetworkFixtures.albums.getValue(albumProperties)

        assertThat(tracks).hasSize(trackProperties.size)
        tracks.zip(trackProperties).forEach { (track, trackProperties) -> trackProperties.check(track) }
    }

    companion object {
        @JvmStatic
        fun albums() = NetworkFixtures.albums.keys
    }
}
