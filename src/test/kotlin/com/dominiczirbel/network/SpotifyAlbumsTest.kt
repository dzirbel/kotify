package com.dominiczirbel.network

import com.dominiczirbel.AlbumProperties
import com.dominiczirbel.Fixtures
import com.dominiczirbel.network.model.SimplifiedTrack
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
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
        val tracksPaging = runBlocking { Spotify.Albums.getAlbumTracks(albumProperties.id) }
        val tracks = runBlocking { tracksPaging.fetchAll<SimplifiedTrack>() }
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
