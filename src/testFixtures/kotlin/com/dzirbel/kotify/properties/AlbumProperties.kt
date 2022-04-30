package com.dzirbel.kotify.properties

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.dzirbel.kotify.Fixtures
import com.dzirbel.kotify.containsAllElementsOf
import com.dzirbel.kotify.network.model.FullSpotifyAlbum
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifySavedAlbum
import kotlinx.coroutines.runBlocking

data class AlbumProperties(
    override val id: String,
    override val name: String,
    val addedAt: String? = null,
    val totalTracks: Int? = null,
    val albumType: SpotifyAlbum.Type? = SpotifyAlbum.Type.ALBUM,
    val genres: List<String> = emptyList(),
) : ObjectProperties(type = "album") {
    fun check(album: SpotifyAlbum) {
        super.check(album)

        albumType?.let { assertThat(album.albumType).isEqualTo(it) }
        assertThat(album.artists).isNotEmpty()
        assertThat(album.availableMarkets).isNotNull()
        assertThat(album.images).isNotEmpty()
        assertThat(album.releaseDate).isNotNull()
        assertThat(album.releaseDatePrecision).isNotNull()
        assertThat(album.restrictions).isNull()
        totalTracks?.let { assertThat(album.totalTracks).isEqualTo(it) }

        if (album is FullSpotifyAlbum) {
            assertThat(album.genres).containsAllElementsOf(genres)
            assertThat(album.popularity).isBetween(0, Fixtures.MAX_POPULARITY)
            assertThat(album.tracks.items).isNotEmpty()
            if (totalTracks != null) {
                assertThat(album.tracks.total).isEqualTo(totalTracks)

                val allTracks = runBlocking { album.tracks.fetchAll<SimplifiedSpotifyTrack>() }
                assertThat(allTracks).hasSize(totalTracks)
            }
        }
    }

    fun check(savedAlbum: SpotifySavedAlbum) {
        check(savedAlbum.album)

        assertThat(addedAt).isNotNull()
        assertThat(savedAlbum.addedAt).isEqualTo(addedAt)
    }
}
