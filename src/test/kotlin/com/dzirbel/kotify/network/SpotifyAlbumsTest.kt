package com.dzirbel.kotify.network

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.Fixtures
import com.dzirbel.kotify.TAG_NETWORK
import com.dzirbel.kotify.network.model.asFlow
import com.dzirbel.kotify.properties.AlbumProperties
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@Tag(TAG_NETWORK)
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
            assertThrows<Spotify.SpotifyError> { Spotify.Albums.getAlbum(Fixtures.notFoundId) }
        }

        assertThat(error.code).isEqualTo(404)
    }

    @Test
    fun getAlbums() {
        val albums = runBlocking { Spotify.Albums.getAlbums(Fixtures.albums.keys.map { it.id }) }

        assertThat(albums.size).isEqualTo(Fixtures.albums.size)
        albums.zip(Fixtures.albums.keys).forEach { (album, albumProperties) -> albumProperties.check(album) }
    }

    @ParameterizedTest
    @MethodSource("albums")
    fun getAlbumTracks(albumProperties: AlbumProperties) {
        val tracks = runBlocking { Spotify.Albums.getAlbumTracks(albumProperties.id).asFlow().toList() }
        val trackProperties = Fixtures.albums.getValue(albumProperties)

        assertThat(tracks).hasSize(trackProperties.size)
        tracks.zip(trackProperties).forEach { (track, trackProperties) -> trackProperties.check(track) }
    }

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun albums() = Fixtures.albums.keys
    }
}
