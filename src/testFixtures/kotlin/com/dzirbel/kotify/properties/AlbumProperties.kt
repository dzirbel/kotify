package com.dzirbel.kotify.properties

import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.network.model.FullAlbum
import com.dzirbel.kotify.network.model.SavedAlbum
import com.dzirbel.kotify.network.model.SimplifiedTrack
import com.google.common.truth.Truth
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

        albumType?.let { Truth.assertThat(album.albumType).isEqualTo(it) }
        Truth.assertThat(album.artists).isNotEmpty()
        Truth.assertThat(album.availableMarkets).isNotNull()
        Truth.assertThat(album.images).isNotEmpty()
        Truth.assertThat(album.releaseDate).isNotNull()
        Truth.assertThat(album.releaseDatePrecision).isNotNull()
        Truth.assertThat(album.restrictions).isNull()
        totalTracks?.let { Truth.assertThat(album.totalTracks).isEqualTo(it) }

        if (album is FullAlbum) {
            Truth.assertThat(album.genres).containsAtLeastElementsIn(genres)
            Truth.assertThat(album.popularity).isIn(0..100)
            Truth.assertThat(album.tracks.items).isNotEmpty()
            totalTracks?.let {
                Truth.assertThat(album.tracks.total).isEqualTo(totalTracks)

                val allTracks = runBlocking { album.tracks.fetchAll<SimplifiedTrack>() }
                Truth.assertThat(allTracks).hasSize(totalTracks)
            }
        }
    }

    fun check(savedAlbum: SavedAlbum) {
        check(savedAlbum.album)

        Truth.assertThat(addedAt).isNotNull()
        Truth.assertThat(savedAlbum.addedAt).isEqualTo(addedAt)
    }
}
