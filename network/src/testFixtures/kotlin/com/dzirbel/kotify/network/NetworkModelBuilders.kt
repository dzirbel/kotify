package com.dzirbel.kotify.network

import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyExternalId
import com.dzirbel.kotify.network.model.SpotifyExternalUrl

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

fun FullSpotifyTrack(
    id: String = "track",
    name: String = "Track",
    popularity: Int = 50,
    trackNumber: Int = 1,
    durationMs: Long = 60_000,
): FullSpotifyTrack {
    return FullSpotifyTrack(
        id = id,
        name = name,
        popularity = popularity,
        trackNumber = trackNumber,
        artists = emptyList(),
        discNumber = 1,
        durationMs = durationMs,
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
