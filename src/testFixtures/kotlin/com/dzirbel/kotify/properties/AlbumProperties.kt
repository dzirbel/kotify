package com.dzirbel.kotify.properties

import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.network.model.FullAlbum
import com.dzirbel.kotify.network.model.SavedAlbum
import com.dzirbel.kotify.network.model.SimplifiedTrack
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking

data class AlbumProperties(
    override val id: String,
    override val name: String,
    val addedAt: String? = null,
    val totalTracks: Int? = null,
    val albumType: Album.Type? = Album.Type.ALBUM,
    val genres: List<String> = emptyList()
) : ObjectProperties(type = "album") {
    fun check(album: Album) {
        super.check(album)

        albumType?.let { assertWithMessage("incorrect album type for $name").that(album.albumType).isEqualTo(it) }
        assertThat(album.artists).isNotEmpty()
        assertThat(album.availableMarkets).isNotNull()
        assertThat(album.images).isNotEmpty()
        assertThat(album.releaseDate).isNotNull()
        assertThat(album.releaseDatePrecision).isNotNull()
        assertThat(album.restrictions).isNull()
        totalTracks?.let { assertWithMessage("incorrect totalTracks for $name").that(album.totalTracks).isEqualTo(it) }

        if (album is FullAlbum) {
            assertThat(album.genres).containsAtLeastElementsIn(genres)
            assertThat(album.popularity).isIn(0..100)
            assertThat(album.tracks.items).isNotEmpty()
            totalTracks?.let {
                assertThat(album.tracks.total).isEqualTo(totalTracks)

                val allTracks = runBlocking { album.tracks.fetchAll<SimplifiedTrack>() }
                assertThat(allTracks).hasSize(totalTracks)
            }
        }
    }

    fun check(savedAlbum: SavedAlbum) {
        check(savedAlbum.album)

        assertThat(addedAt).isNotNull()
        assertThat(savedAlbum.addedAt).isEqualTo(addedAt)
    }
}
