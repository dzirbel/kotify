package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.AlbumType
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.network.FullSpotifyAlbum
import com.dzirbel.kotify.network.FullSpotifyArtist
import com.dzirbel.kotify.network.FullSpotifyArtistList
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import java.time.Instant

@Suppress("FunctionNaming")
fun ArtistList(count: Int): List<Artist> {
    val networkArtists = FullSpotifyArtistList(count = count)
    val fetchTime = Instant.now()
    return KotifyDatabase.blockingTransaction {
        networkArtists.map { requireNotNull(ArtistRepository.convertToDB(it, fetchTime)) }
    }
}

fun Artist(fullUpdateTime: Instant? = null, albumsFetched: Instant? = null): Artist {
    val fetchTime = Instant.now()
    return KotifyDatabase.blockingTransaction {
        val artist = requireNotNull(ArtistRepository.convertToDB(FullSpotifyArtist(), fetchTime))
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
            ArtistAlbum.findOrCreate(
                artistId = artistId,
                albumId = it.id.value,
                albumGroup = AlbumType.ALBUM,
            )
        }
    }
}

@Suppress("FunctionNaming")
fun AlbumList(count: Int, fullUpdateTime: Instant? = null): List<Album> {
    val networkAlbums = List(count) { FullSpotifyAlbum(id = "album-$it", name = "Album $it") }
    val fetchTime = Instant.now()
    return KotifyDatabase.blockingTransaction {
        networkAlbums.map { networkAlbum ->
            val album = requireNotNull(AlbumRepository.convertToDB(networkAlbum, fetchTime))
            fullUpdateTime?.let { album.fullUpdatedTime = it }

            album
        }
    }
}
