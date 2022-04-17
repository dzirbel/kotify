package com.dzirbel.kotify

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.network.model.FullSpotifyAlbum
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.Paging
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyExternalId
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyFollowers

/**
 * A collection of convenience functions which construct various model classes with mocked data for use in tests.
 */
object FixtureModels {
    fun artists(count: Int): List<Artist> {
        val networkArtists = networkArtists(count = count)
        return testTransaction {
            networkArtists.map { requireNotNull(Artist.from(it)) }
        }
    }

    fun artist(): Artist {
        return testTransaction { requireNotNull(Artist.from(networkArtist())) }
    }

    private fun networkArtists(count: Int): List<SpotifyArtist> {
        return List(count) { index ->
            networkArtist(id = "artist-$index", name = "Artist $index")
        }
    }

    private fun networkArtist(
        id: String = "artist",
        name: String = "Artist",
        followers: Int = 100,
        popularity: Int = 50,
    ): SpotifyArtist {
        return FullSpotifyArtist(
            id = id,
            name = name,
            externalUrls = SpotifyExternalUrl(),
            href = "href",
            type = "artist",
            uri = "uri",
            followers = SpotifyFollowers(total = followers),
            genres = listOf("genre"),
            images = listOf(),
            popularity = popularity,
        )
    }

    fun artistAlbums(artistId: String, count: Int): List<ArtistAlbum> {
        val albums = albums(count)
        return testTransaction {
            albums.map {
                ArtistAlbum.from(
                    artistId = artistId,
                    albumId = requireNotNull(it.id.value),
                    albumGroup = SpotifyAlbum.Type.ALBUM,
                )
            }
        }
    }

    fun albums(count: Int): List<Album> {
        val networkAlbums = List(count) { networkAlbum(id = "album-$it", name = "Album $it") }
        return testTransaction {
            networkAlbums.map { requireNotNull(Album.from(it)) }
        }
    }

    private fun networkAlbum(
        id: String = "album",
        name: String = "Album",
        popularity: Int = 50,
        tracks: List<SimplifiedSpotifyTrack> = networkTracks(count = 10),
    ): SpotifyAlbum {
        return FullSpotifyAlbum(
            id = id,
            name = name,
            externalUrls = emptyMap(),
            href = "href",
            type = "album",
            uri = "uri",
            images = listOf(),
            releaseDate = "",
            releaseDatePrecision = "",
            copyrights = listOf(),
            externalIds = SpotifyExternalId(),
            genres = listOf("genre"),
            label = "",
            popularity = popularity,
            artists = listOf(),
            tracks = Paging(
                items = tracks,
                href = "href",
                limit = tracks.size,
                offset = 0,
                total = tracks.size,
            ),
        )
    }

    private fun networkTracks(count: Int): List<SimplifiedSpotifyTrack> {
        return List(count) { index ->
            networkTrack(id = "track-$index", name = "Track $index", trackNumber = index + 1)
        }
    }

    private fun networkTrack(
        id: String = "track",
        name: String = "Track",
        popularity: Int = 50,
        trackNumber: Int = 1,
    ): SimplifiedSpotifyTrack {
        return SimplifiedSpotifyTrack(
            id = id,
            name = name,
            popularity = popularity,
            trackNumber = trackNumber,
            artists = listOf(),
            discNumber = 1,
            durationMs = 60_000,
            explicit = false,
            externalUrls = SpotifyExternalUrl(),
            href = "href",
            isLocal = false,
            type = "track",
            uri = "uri",
            externalIds = SpotifyExternalId(),
        )
    }
}
