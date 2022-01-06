package com.dzirbel.kotify.cache

import com.dzirbel.kotify.cache.SpotifyCache.GlobalObjects
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.runBlocking

object LibraryCache {
    data class CachedPlaylist(
        val id: String,
        val playlist: SpotifyPlaylist?,
        val updated: Long?,
        val tracks: GlobalObjects.PlaylistTracks?,
        val tracksUpdated: Long?
    )

    val savedPlaylists: Set<String>?
        get() = SpotifyCache.getCached<GlobalObjects.SavedPlaylists>(GlobalObjects.SavedPlaylists.ID)?.ids

    val playlistsUpdated: Long?
        get() = SpotifyCache.lastUpdated(GlobalObjects.SavedPlaylists.ID)

    val playlists: Map<String, SpotifyPlaylist?>?
        get() = savedPlaylists?.let { ids -> ids.zipToMap(SpotifyCache.getCached<SpotifyPlaylist>(ids)) }

    val playlistTracks: Map<String, GlobalObjects.PlaylistTracks?>?
        get() = savedPlaylists
            ?.let { playlistIds ->
                val playlistTrackIds = playlistIds
                    .map { playlistId -> GlobalObjects.PlaylistTracks.idFor(playlistId = playlistId) }

                playlistIds.zipToMap(SpotifyCache.getCached<GlobalObjects.PlaylistTracks>(playlistTrackIds))
            }

    val cachedPlaylists: List<CachedPlaylist>?
        get() {
            return playlists?.toList()?.let { playlists ->
                val playlistTracks = LibraryCache.playlistTracks

                // batch calls for last updates
                val updated = SpotifyCache.lastUpdated(
                    playlists.map { it.first }
                        .plus(playlists.map { GlobalObjects.PlaylistTracks.idFor(playlistId = it.first) })
                )
                check(updated.size == playlists.size * 2)

                playlists.mapIndexed { index, (id, playlist) ->
                    CachedPlaylist(
                        id = id,
                        playlist = playlist,
                        updated = updated[index],
                        tracks = playlistTracks?.get(id),
                        tracksUpdated = updated[index + playlists.size]
                    )
                }
            }
        }

    // TODO also include cache times of individual artists, albums, etc?
    val lastUpdated: Long?
        get() {
            val ids = listOf(GlobalObjects.SavedPlaylists.ID)

            val savedAlbumsUpdated = runBlocking { SavedAlbumRepository.libraryUpdated() }

            val values = SpotifyCache.lastUpdated(ids)
                .plus(savedAlbumsUpdated?.toEpochMilli())
                .filterNotNull()

            // return null if any values are not cached
            if (values.size < ids.size) return null

            return values.minOrNull()
        }

    fun playlistsContaining(trackId: String): Set<String>? {
        return playlistTracks
            ?.filterValues { playlistTracks -> playlistTracks?.trackIds?.any { it == trackId } == true }
            ?.keys
    }

    fun playlistTracks(playlistId: String): List<SpotifyPlaylistTrack?>? {
        val playlistTrackId = GlobalObjects.PlaylistTracks.idFor(playlistId = playlistId)
        return SpotifyCache.getCached<GlobalObjects.PlaylistTracks>(playlistTrackId)
            ?.playlistTrackIds
            ?.let { SpotifyCache.getCached<SpotifyPlaylistTrack>(ids = it) }
    }

    fun clear() {
        SpotifyCache.invalidate(GlobalObjects.SavedPlaylists.ID)
    }
}
