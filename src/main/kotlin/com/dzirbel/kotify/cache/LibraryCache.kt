package com.dzirbel.kotify.cache

import com.dzirbel.kotify.cache.SpotifyCache.GlobalObjects
import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.network.model.Artist
import com.dzirbel.kotify.network.model.Playlist
import com.dzirbel.kotify.network.model.PlaylistTrack
import com.dzirbel.kotify.network.model.Track
import com.dzirbel.kotify.util.zipToMap

object LibraryCache {
    val savedArtists: List<String>?
        get() = SpotifyCache.getCached<GlobalObjects.SavedArtists>(GlobalObjects.SavedArtists.ID)?.ids

    val artistsUpdated: Long?
        get() = SpotifyCache.lastUpdated(GlobalObjects.SavedArtists.ID)

    val artists: Map<String, Artist?>?
        get() = savedArtists?.let { ids -> ids.zipToMap(SpotifyCache.getCached<Artist>(ids)) }

    val artistAlbums: Map<String, List<String>?>?
        get() = savedArtists
            ?.let { artistIds ->
                val artistAlbumIds = artistIds.map { artistId -> GlobalObjects.ArtistAlbums.idFor(artistId = artistId) }
                artistIds.zipToMap(
                    SpotifyCache.getCached<GlobalObjects.ArtistAlbums>(artistAlbumIds).map { it?.albumIds }
                )
            }

    val savedAlbums: List<String>?
        get() = SpotifyCache.getCached<GlobalObjects.SavedAlbums>(GlobalObjects.SavedAlbums.ID)?.ids

    val albumsUpdated: Long?
        get() = SpotifyCache.lastUpdated(GlobalObjects.SavedAlbums.ID)

    val albums: Map<String, Album?>?
        get() = savedAlbums?.let { ids -> ids.zipToMap(SpotifyCache.getCached<Album>(ids)) }

    val savedPlaylists: List<String>?
        get() = SpotifyCache.getCached<GlobalObjects.SavedPlaylists>(GlobalObjects.SavedPlaylists.ID)?.ids

    val playlistsUpdated: Long?
        get() = SpotifyCache.lastUpdated(GlobalObjects.SavedPlaylists.ID)

    val playlists: Map<String, Playlist?>?
        get() = savedPlaylists?.let { ids -> ids.zipToMap(SpotifyCache.getCached<Playlist>(ids)) }

    val playlistTracks: Map<String, GlobalObjects.PlaylistTracks?>?
        get() = savedPlaylists
            ?.let { playlistIds ->
                val playlistTrackIds = playlistIds
                    .map { playlistId -> GlobalObjects.PlaylistTracks.idFor(playlistId = playlistId) }

                playlistIds.zipToMap(SpotifyCache.getCached<GlobalObjects.PlaylistTracks>(playlistTrackIds))
            }

    val savedTracks: List<String>?
        get() = SpotifyCache.getCached<GlobalObjects.SavedTracks>(GlobalObjects.SavedTracks.ID)?.ids

    val tracks: Map<String, Track?>?
        get() = savedTracks?.let { ids -> ids.zipToMap(SpotifyCache.getCached<Track>(ids)) }

    val tracksUpdated: Long?
        get() = SpotifyCache.lastUpdated(GlobalObjects.SavedTracks.ID)

    fun playlistsContaining(trackId: String): Set<String>? {
        return playlistTracks
            ?.filterValues { playlistTracks -> playlistTracks?.trackIds?.any { it == trackId } == true }
            ?.keys
    }

    fun playlistTracks(playlistId: String): List<PlaylistTrack?>? {
        val playlistTrackId = GlobalObjects.PlaylistTracks.idFor(playlistId = playlistId)
        return SpotifyCache.getCached<GlobalObjects.PlaylistTracks>(playlistTrackId)
            ?.playlistTrackIds
            ?.let { SpotifyCache.getCached<PlaylistTrack>(ids = it) }
    }
}
