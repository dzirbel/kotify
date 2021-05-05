package com.dzirbel.kotify.cache

import com.dzirbel.kotify.cache.SpotifyCache.GlobalObjects
import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.network.model.Artist
import com.dzirbel.kotify.network.model.Playlist
import com.dzirbel.kotify.network.model.PlaylistTrack
import com.dzirbel.kotify.network.model.Track
import com.dzirbel.kotify.util.zipToMap

object LibraryCache {
    data class CachedArtist(
        val id: String,
        val artist: Artist?,
        val updated: Long?,
        val albums: List<String>?,
        val albumsUpdated: Long?
    )

    data class CachedAlbum(
        val id: String,
        val album: Album?,
        val updated: Long?
    )

    data class CachedPlaylist(
        val id: String,
        val playlist: Playlist?,
        val updated: Long?,
        val tracks: GlobalObjects.PlaylistTracks?,
        val tracksUpdated: Long?
    )

    data class CachedTrack(
        val id: String,
        val track: Track?,
        val updated: Long?
    )

    val savedArtists: Set<String>?
        get() = SpotifyCache.getCached<GlobalObjects.SavedArtists>(GlobalObjects.SavedArtists.ID)?.ids

    val artistsUpdated: Long?
        get() = SpotifyCache.lastUpdated(GlobalObjects.SavedArtists.ID)

    val artists: Map<String, Artist?>?
        get() = savedArtists?.let { ids -> ids.zipToMap(SpotifyCache.getCached<Artist>(ids)) }

    val cachedArtists: List<CachedArtist>?
        get() {
            return artists?.toList()?.let { artists ->
                val artistAlbums = LibraryCache.artistAlbums

                // batch calls for last updates
                val updated = SpotifyCache.lastUpdated(
                    artists.map { it.first }
                        .plus(artists.map { GlobalObjects.ArtistAlbums.idFor(artistId = it.first) })
                )
                check(updated.size == artists.size * 2)

                artists.mapIndexed { index, (id, artist) ->
                    CachedArtist(
                        id = id,
                        artist = artist,
                        updated = updated[index],
                        albums = artistAlbums?.get(id),
                        albumsUpdated = updated[index + artists.size]
                    )
                }
            }
        }

    val artistAlbums: Map<String, List<String>?>?
        get() = savedArtists
            ?.let { artistIds ->
                val artistAlbumIds = artistIds.map { artistId -> GlobalObjects.ArtistAlbums.idFor(artistId = artistId) }
                artistIds.zipToMap(
                    SpotifyCache.getCached<GlobalObjects.ArtistAlbums>(artistAlbumIds).map { it?.albumIds }
                )
            }

    val savedAlbums: Set<String>?
        get() = SpotifyCache.getCached<GlobalObjects.SavedAlbums>(GlobalObjects.SavedAlbums.ID)?.ids

    val albumsUpdated: Long?
        get() = SpotifyCache.lastUpdated(GlobalObjects.SavedAlbums.ID)

    val albums: Map<String, Album?>?
        get() = savedAlbums?.let { ids -> ids.zipToMap(SpotifyCache.getCached<Album>(ids)) }

    val cachedAlbums: List<CachedAlbum>?
        get() {
            return albums?.toList()?.let { albums ->
                // batch calls for last updates
                val updated = SpotifyCache.lastUpdated(albums.map { it.first })

                albums.mapIndexed { index, (id, album) ->
                    CachedAlbum(id = id, album = album, updated = updated[index])
                }
            }
        }

    val savedPlaylists: Set<String>?
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

    val savedTracks: Set<String>?
        get() = SpotifyCache.getCached<GlobalObjects.SavedTracks>(GlobalObjects.SavedTracks.ID)?.ids

    val tracks: Map<String, Track?>?
        get() = savedTracks?.let { ids -> ids.zipToMap(SpotifyCache.getCached<Track>(ids)) }

    val tracksUpdated: Long?
        get() = SpotifyCache.lastUpdated(GlobalObjects.SavedTracks.ID)

    val cachedTracks: List<CachedTrack>?
        get() {
            return tracks?.toList()?.let { tracks ->
                // batch calls for last updates
                val updated = SpotifyCache.lastUpdated(tracks.map { it.first })

                tracks.mapIndexed { index, (id, track) ->
                    CachedTrack(id = id, track = track, updated = updated[index])
                }
            }
        }

    // TODO also include cache times of individual artists, albums, etc?
    val lastUpdated: Long?
        get() {
            val ids = listOf(
                GlobalObjects.SavedArtists.ID,
                GlobalObjects.SavedAlbums.ID,
                GlobalObjects.SavedPlaylists.ID,
                GlobalObjects.SavedTracks.ID,
                GlobalObjects.CURRENT_USER_ID
            )

            val values = SpotifyCache.lastUpdated(ids).filterNotNull()

            // return null if any values are not cached
            if (values.size < ids.size) return null

            return values.minOrNull()
        }

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

    fun clear() {
        SpotifyCache.invalidate(GlobalObjects.SavedAlbums.ID)
        SpotifyCache.invalidate(GlobalObjects.SavedAlbums.ID)
        SpotifyCache.invalidate(GlobalObjects.SavedTracks.ID)
        SpotifyCache.invalidate(GlobalObjects.SavedPlaylists.ID)
        SpotifyCache.invalidate(GlobalObjects.CURRENT_USER_ID)
    }
}
