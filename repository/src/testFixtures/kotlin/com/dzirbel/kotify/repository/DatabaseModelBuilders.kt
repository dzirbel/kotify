package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.network.FullSpotifyAlbum
import com.dzirbel.kotify.network.FullSpotifyArtist
import com.dzirbel.kotify.network.FullSpotifyArtistList
import com.dzirbel.kotify.network.model.SpotifyAlbum
import java.time.Instant

@Suppress("FunctionNaming")
fun ArtistList(count: Int): List<Artist> {
    val networkArtists = FullSpotifyArtistList(count = count)
    return KotifyDatabase.blockingTransaction {
        networkArtists.map { requireNotNull(Artist.from(it)) }
    }
}

fun Artist(fullUpdateTime: Instant? = null, albumsFetched: Instant? = null): Artist {
    return KotifyDatabase.blockingTransaction {
        val artist = requireNotNull(Artist.from(FullSpotifyArtist()))
        fullUpdateTime?.let { artist.fullUpdatedTime = it }
        albumsFetched?.let { artist.albumsFetched = it }

        artist
    }
}

@Suppress("FunctionNaming")
fun ArtistAlbumList(artistId: String, count: Int, fullUpdateTime: Instant? = null): List<ArtistAlbum> {
    val albums = AlbumList(count, fullUpdateTime = fullUpdateTime)
    return KotifyDatabase.blockingTransaction {
        albums.map {
            ArtistAlbum.from(
                artistId = artistId,
                albumId = it.id.value,
                albumGroup = SpotifyAlbum.Type.ALBUM,
            )
        }
    }
}

@Suppress("FunctionNaming")
fun AlbumList(count: Int, fullUpdateTime: Instant? = null): List<Album> {
    val networkAlbums = List(count) { FullSpotifyAlbum(id = "album-$it", name = "Album $it") }
    return KotifyDatabase.blockingTransaction {
        networkAlbums.map { networkAlbum ->
            val album = requireNotNull(Album.from(networkAlbum))
            fullUpdateTime?.let { album.fullUpdatedTime = it }

            album
        }
    }
}
