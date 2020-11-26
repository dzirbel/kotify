package com.dominiczirbel.network

import com.dominiczirbel.AlbumProperties
import com.dominiczirbel.Fixtures
import com.dominiczirbel.Secrets
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class SpotifyAlbumsTest {
    @ParameterizedTest
    @MethodSource("albums")
    fun getAlbum(albumProperties: AlbumProperties) {
        val album = runBlocking { Spotify.Albums.getAlbum(albumProperties.id) }
        albumProperties.check(album)
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
        val tracks = runBlocking { Spotify.Albums.getAlbumTracks(albumProperties.id) }
        val trackProperties = Fixtures.albums.getValue(albumProperties)
        assertThat(tracks.items.size).isEqualTo(trackProperties.size)
        tracks.items.zip(trackProperties).forEach { (track, trackProperties) -> trackProperties.check(track) }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        @Suppress("unused")
        fun setup() {
            Secrets.load()
            Secrets.authenticate()
        }

        @JvmStatic
        @Suppress("unused")
        fun albums() = Fixtures.albums.keys
    }
}
