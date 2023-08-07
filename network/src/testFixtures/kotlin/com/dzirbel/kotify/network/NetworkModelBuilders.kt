package com.dzirbel.kotify.network

import com.dzirbel.kotify.network.model.FullSpotifyAlbum
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.FullSpotifyPlaylist
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.Paging
import com.dzirbel.kotify.network.model.PublicSpotifyUser
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SimplifiedSpotifyArtist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyExternalId
import com.dzirbel.kotify.network.model.SpotifyExternalUrl
import com.dzirbel.kotify.network.model.SpotifyFollowers
import com.dzirbel.kotify.network.model.SpotifyPlaybackContext
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.time.Duration.Companion.minutes

@Suppress("FunctionNaming")
fun FullSpotifyArtistList(count: Int): List<FullSpotifyArtist> {
    return List(count) { index ->
        FullSpotifyArtist(id = "artist-$index", name = "Artist $index")
    }
}

fun FullSpotifyArtist(
    id: String = "artist",
    name: String = "Artist",
    followers: Int = 100,
    popularity: Int = 50,
): FullSpotifyArtist {
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

fun SimplifiedSpotifyArtist(id: String = "artist", name: String = "Artist"): SimplifiedSpotifyArtist {
    return SimplifiedSpotifyArtist(
        id = id,
        name = name,
        externalUrls = SpotifyExternalUrl(),
        href = "href",
        type = "artist",
        uri = "uri",
    )
}

fun FullSpotifyAlbum(
    id: String = "album",
    name: String = "Album",
    popularity: Int = 50,
    tracks: List<SimplifiedSpotifyTrack> = SimplifiedSpotifyTrackList(count = 10),
): FullSpotifyAlbum {
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

fun SimplifiedSpotifyAlbum(id: String = "album", name: String = "Album"): SimplifiedSpotifyAlbum {
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

@Suppress("FunctionNaming")
fun SimplifiedSpotifyTrackList(count: Int): List<SimplifiedSpotifyTrack> {
    return List(count) { index ->
        SimplifiedSpotifyTrack(id = "track-$index", name = "Track $index", trackNumber = index + 1)
    }
}

fun SimplifiedSpotifyTrack(
    id: String = "track",
    name: String = "Track",
    popularity: Int = 50,
    trackNumber: Int = 1,
    durationMs: Long = 1.minutes.inWholeMilliseconds,
    album: SimplifiedSpotifyAlbum? = null,
    artists: List<SimplifiedSpotifyArtist> = emptyList(),
): SimplifiedSpotifyTrack {
    return SimplifiedSpotifyTrack(
        id = id,
        name = name,
        popularity = popularity,
        trackNumber = trackNumber,
        album = album,
        artists = artists,
        discNumber = 1,
        durationMs = durationMs,
        explicit = false,
        externalUrls = SpotifyExternalUrl(),
        href = "href",
        isLocal = false,
        type = "track",
        uri = "uri",
        externalIds = SpotifyExternalId(),
    )
}

fun FullSpotifyTrack(
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
        album = SimplifiedSpotifyAlbum(),
    )
}

fun FullSpotifyPlaylist(
    id: String,
    name: String,
    tracks: List<SimplifiedSpotifyTrack>,
    trackAddedAt: List<String?>? = null,
    followers: Int,
): FullSpotifyPlaylist {
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
            items = tracks.mapIndexed { index, track ->
                SpotifyPlaylistTrack(
                    addedAt = trackAddedAt?.getOrNull(index),
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

fun SpotifyPlaybackContext(uri: String): SpotifyPlaybackContext {
    return SpotifyPlaybackContext(
        uri = uri,
        externalUrls = SpotifyExternalUrl(),
        href = "href",
        type = "type",
    )
}
