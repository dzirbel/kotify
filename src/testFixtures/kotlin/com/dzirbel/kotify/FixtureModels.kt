package com.dzirbel.kotify

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.network.model.FullSpotifyAlbum
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.FullSpotifyPlaylist
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.Paging
import com.dzirbel.kotify.network.model.PublicSpotifyUser
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyExternalId
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyFollowers
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.time.Instant

/**
 * A collection of convenience functions which construct various model classes with mocked data for use in tests.
 */
@Suppress("TooManyFunctions")
object FixtureModels {
    fun artists(count: Int): List<Artist> {
        val networkArtists = networkArtists(count = count)
        return testTransaction {
            networkArtists.map { requireNotNull(Artist.from(it)) }
        }
    }

    fun artist(fullUpdateTime: Instant? = null, albumsFetched: Instant? = null): Artist {
        return testTransaction {
            val artist = requireNotNull(Artist.from(networkArtist()))
            fullUpdateTime?.let { artist.fullUpdatedTime = it }
            albumsFetched?.let { artist.albumsFetched = it }

            artist
        }
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
            images = emptyList(),
            popularity = popularity,
        )
    }

    fun artistAlbums(artistId: String, count: Int, fullUpdateTime: Instant? = null): List<ArtistAlbum> {
        val albums = albums(count, fullUpdateTime = fullUpdateTime)
        return testTransaction {
            albums.map {
                ArtistAlbum.from(
                    artistId = artistId,
                    albumId = it.id.value,
                    albumGroup = SpotifyAlbum.Type.ALBUM,
                )
            }
        }
    }

    fun albums(count: Int, fullUpdateTime: Instant? = null): List<Album> {
        val networkAlbums = List(count) { networkAlbum(id = "album-$it", name = "Album $it") }
        return testTransaction {
            networkAlbums.map { networkAlbum ->
                val album = requireNotNull(Album.from(networkAlbum))
                fullUpdateTime?.let { album.fullUpdatedTime = it }

                album
            }
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
            images = emptyList(),
            releaseDate = "",
            releaseDatePrecision = "",
            copyrights = emptyList(),
            externalIds = SpotifyExternalId(),
            genres = listOf("genre"),
            label = "",
            popularity = popularity,
            artists = emptyList(),
            tracks = Paging(
                items = tracks,
                href = "href",
                limit = tracks.size,
                offset = 0,
                total = tracks.size,
            ),
        )
    }

    private fun networkSimplifiedAlbum(
        id: String = "album",
        name: String = "Album",
    ): SimplifiedSpotifyAlbum {
        return SimplifiedSpotifyAlbum(
            id = id,
            name = name,
            externalUrls = emptyMap(),
            href = "href",
            type = "album",
            uri = "uri",
            images = emptyList(),
            releaseDate = "",
            releaseDatePrecision = "",
            artists = emptyList(),
        )
    }

    private fun networkTracks(count: Int): List<SimplifiedSpotifyTrack> {
        return List(count) { index ->
            networkTrack(id = "track-$index", name = "Track $index", trackNumber = index + 1)
        }
    }

    fun networkTrack(
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
            artists = emptyList(),
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

    fun networkFullTrack(
        id: String = "track",
        name: String = "Track",
        popularity: Int = 50,
        trackNumber: Int = 1,
    ): FullSpotifyTrack {
        return FullSpotifyTrack(
            id = id,
            name = name,
            popularity = popularity,
            trackNumber = trackNumber,
            artists = emptyList(),
            discNumber = 1,
            durationMs = 60_000,
            explicit = false,
            externalUrls = SpotifyExternalUrl(),
            href = "href",
            isLocal = false,
            type = "track",
            uri = "uri",
            externalIds = SpotifyExternalId(),
            album = networkSimplifiedAlbum(),
        )
    }

    fun playlist(
        id: String = "id",
        name: String = "Playlist",
        trackCount: Int = 25,
        followers: Int = 10,
        fullUpdateTime: Instant? = null,
    ): Playlist {
        val networkPlaylist = networkPlaylist(
            id = id,
            name = name,
            tracks = networkTracks(count = trackCount),
            followers = followers,
        )
        return testTransaction {
            val playlist = requireNotNull(Playlist.from(networkPlaylist))
            fullUpdateTime?.let { playlist.fullUpdatedTime = it }

            playlist
        }
    }

    private fun networkPlaylist(
        id: String,
        name: String,
        tracks: List<SimplifiedSpotifyTrack>,
        followers: Int,
    ): SpotifyPlaylist {
        val user = PublicSpotifyUser(
            displayName = "user",
            externalUrls = SpotifyExternalUrl(),
            href = "href",
            id = "user",
            type = "user",
            uri = "uri",
        )

        return FullSpotifyPlaylist(
            id = id,
            name = name,
            tracks = Paging(
                items = tracks.map { track ->
                    SpotifyPlaylistTrack(
                        addedAt = null,
                        addedBy = user,
                        isLocal = false,
                        trackOrEpisode = Json.encodeToJsonElement(track),
                    )
                },
                href = "href",
                limit = tracks.size,
                offset = 0,
                total = tracks.size,
            ),
            followers = SpotifyFollowers(total = followers),
            href = "href",
            type = "playlist",
            uri = "uri",
            collaborative = false,
            description = null,
            externalUrls = SpotifyExternalUrl(),
            images = emptyList(),
            owner = user,
            snapshotId = "snapshot",
        )
    }
}
