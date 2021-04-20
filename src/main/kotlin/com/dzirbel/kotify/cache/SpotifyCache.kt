package com.dzirbel.kotify.cache

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.network.model.Artist
import com.dzirbel.kotify.network.model.Episode
import com.dzirbel.kotify.network.model.FullAlbum
import com.dzirbel.kotify.network.model.FullArtist
import com.dzirbel.kotify.network.model.FullEpisode
import com.dzirbel.kotify.network.model.FullPlaylist
import com.dzirbel.kotify.network.model.FullShow
import com.dzirbel.kotify.network.model.FullTrack
import com.dzirbel.kotify.network.model.Playlist
import com.dzirbel.kotify.network.model.PrivateUser
import com.dzirbel.kotify.network.model.PublicUser
import com.dzirbel.kotify.network.model.SavedAlbum
import com.dzirbel.kotify.network.model.SavedTrack
import com.dzirbel.kotify.network.model.Show
import com.dzirbel.kotify.network.model.SimplifiedAlbum
import com.dzirbel.kotify.network.model.SimplifiedArtist
import com.dzirbel.kotify.network.model.SimplifiedEpisode
import com.dzirbel.kotify.network.model.SimplifiedPlaylist
import com.dzirbel.kotify.network.model.SimplifiedShow
import com.dzirbel.kotify.network.model.SimplifiedTrack
import com.dzirbel.kotify.network.model.Track
import com.dzirbel.kotify.network.model.User
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

object SpotifyCache {
    // TODO clear from cache on log out
    object GlobalObjects {
        const val CURRENT_USER_ID = "current-user"

        @Serializable
        data class SavedAlbums(val ids: List<String>) : CacheableObject {
            override val id = ID

            companion object {
                const val ID = "saved-albums"
            }
        }

        @Serializable
        data class SavedArtists(val ids: List<String>) : CacheableObject {
            override val id = ID

            companion object {
                const val ID = "saved-artists"
            }
        }

        @Serializable
        data class SavedPlaylists(val ids: List<String>) : CacheableObject {
            override val id = ID

            companion object {
                const val ID = "saved-playlists"
            }
        }

        @Serializable
        data class SavedTracks(val ids: List<String>) : CacheableObject {
            override val id = ID

            companion object {
                const val ID = "saved-tracks"
            }
        }

        @Serializable
        data class ArtistAlbums(val artistId: String, val albumIds: List<String>) : CacheableObject {
            override val id
                get() = idFor(artistId)

            companion object {
                fun idFor(artistId: String) = "artist-albums-$artistId"
            }
        }
    }

    private val cacheFile = Application.cacheDir.resolve("cache.json")

    private val cache = Cache(
        file = cacheFile,

        saveOnChange = true,

        ttlStrategy = CacheTTLStrategy.AlwaysValid,

        eventHandler = { events ->
            if (events.any { it is CacheEvent.Save }) {
                onSave()
            }

            Logger.Cache.handleCacheEvents(events)
        },

        // TODO handle case where simplified object has been updated, but full is now out of date
        replacementStrategy = object : CacheReplacementStrategy {
            /**
             * Checks that an inferior [Simplified] type will not replace a superior [Full] type, returning a non-null
             * value when both [current] and [new] are [Base].
             *
             * This is a convenience function to check the common case where the cache may include both a simplified
             * version of a model and a full version; in this case we need to ensure the full version is never replaced
             * by a simplified version. For example, [Album] objects come in both a [FullAlbum] and [SimplifiedAlbum]
             * variant
             *
             * This also checks that [current] is a [Base] object XOR [new] is a [Base] object; this is a sanity check
             * that new objects are not replacing entirely different types of objects in the cache.
             */
            private inline fun <
                reified Base : Any,
                reified Simplified : Base,
                reified Full : Base
                > checkReplacement(current: Any, new: Any): Boolean? {
                require(current is Base == new is Base) {
                    "attempted to replace an object of type ${current::class.qualifiedName} with an object of " +
                        "type ${new::class.qualifiedName}"
                }
                if (current is Base) {
                    return !(new is Simplified && current is Full)
                }
                return null
            }

            @Suppress("ReturnCount")
            override fun replace(current: Any, new: Any): Boolean {
                checkReplacement<Album, SimplifiedAlbum, FullAlbum>(current, new)?.let { return it }
                checkReplacement<Artist, SimplifiedArtist, FullArtist>(current, new)?.let { return it }
                checkReplacement<Episode, SimplifiedEpisode, FullEpisode>(current, new)?.let { return it }
                checkReplacement<Playlist, SimplifiedPlaylist, FullPlaylist>(current, new)?.let { return it }
                checkReplacement<Show, SimplifiedShow, FullShow>(current, new)?.let { return it }
                checkReplacement<Track, SimplifiedTrack, FullTrack>(current, new)?.let { return it }
                checkReplacement<User, PublicUser, PrivateUser>(current, new)?.let { return it }

                return true
            }
        }
    )

    val size: Int
        get() = cache.size

    var sizeOnDisk by mutableStateOf(0L)
        private set

    init {
        // trigger initializing sizeOnDisk in the background
        onSave()
    }

    private fun onSave() {
        GlobalScope.launch {
            sizeOnDisk = cacheFile.length()
        }
    }

    /**
     * Loads the cache from disk, overwriting any values currently in memory.
     */
    fun load() {
        cache.load()
    }

    /**
     * Clears the cache, both in-memory and on disk.
     */
    fun clear() {
        cache.clear()
    }

    fun invalidate(id: String) {
        cache.invalidate(id)
    }

    fun lastUpdated(id: String): Long? {
        return cache.getCached(id)?.cacheTime
    }

    fun put(obj: CacheableObject) {
        cache.put(obj)
    }

    object Albums {
        // most batched calls have a maximum of 50; for albums the maximum is 20
        private const val MAX_ALBUM_IDS_LOOKUP = 20

        suspend fun getAlbum(id: String): Album = cache.get<Album>(id) { Spotify.Albums.getAlbum(id) }
        suspend fun getFullAlbum(id: String): FullAlbum = cache.get(id) { Spotify.Albums.getAlbum(id) }

        suspend fun getAlbums(ids: List<String>): List<Album> {
            return cache.getAll<Album>(ids = ids) { missingIds ->
                missingIds.chunked(size = MAX_ALBUM_IDS_LOOKUP)
                    .flatMap { idsChunk -> Spotify.Albums.getAlbums(ids = idsChunk) }
            }
        }

        suspend fun saveAlbum(id: String) {
            Spotify.Library.saveAlbums(listOf(id))

            cache.getCached(GlobalObjects.SavedAlbums.ID)?.let { albums ->
                val savedAlbums = albums.obj as GlobalObjects.SavedAlbums

                // don't update the cache time since we haven't actually refreshed the value from the remote
                cache.put(
                    value = savedAlbums.copy(ids = savedAlbums.ids.plus(id)),
                    cacheTime = albums.cacheTime
                )
            }
        }

        suspend fun unsaveAlbum(id: String) {
            Spotify.Library.removeAlbums(listOf(id))

            cache.getCached(GlobalObjects.SavedAlbums.ID)?.let { albums ->
                val savedAlbums = albums.obj as GlobalObjects.SavedAlbums

                // don't update the cache time since we haven't actually refreshed the value from the remote
                cache.put(
                    value = savedAlbums.copy(ids = savedAlbums.ids.minus(id)),
                    cacheTime = albums.cacheTime
                )
            }
        }

        suspend fun getSavedAlbums(): List<String> {
            cache.getCachedValue<GlobalObjects.SavedAlbums>(GlobalObjects.SavedAlbums.ID)?.ids?.let { return it }

            val albums = Spotify.Library.getSavedAlbums(limit = Spotify.MAX_LIMIT).fetchAll<SavedAlbum>()
            val savedAlbums = GlobalObjects.SavedAlbums(ids = albums.map { it.album.id })
            cache.putAll(albums.plus(savedAlbums))

            return savedAlbums.ids
        }
    }

    object Artists {
        suspend fun getArtist(id: String): Artist = cache.get<Artist>(id) { Spotify.Artists.getArtist(id) }
        suspend fun getFullArtist(id: String): FullArtist = cache.get(id) { Spotify.Artists.getArtist(id) }

        suspend fun getArtistAlbums(artistId: String): List<Album> {
            cache.getCachedValue<GlobalObjects.ArtistAlbums>(
                id = GlobalObjects.ArtistAlbums.idFor(artistId = artistId)
            )?.let { return Albums.getAlbums(ids = it.albumIds) }

            return Spotify.Artists.getArtistAlbums(id = artistId).fetchAll<SimplifiedAlbum>()
                .also { albums ->
                    val artistAlbums = GlobalObjects.ArtistAlbums(
                        artistId = artistId,
                        albumIds = albums.map { requireNotNull(it.id) }
                    )
                    cache.putAll(albums.plus(artistAlbums))
                }
        }

        suspend fun getSavedArtists(): List<String> {
            cache.getCachedValue<GlobalObjects.SavedArtists>(GlobalObjects.SavedArtists.ID)
                ?.let { return it.ids }

            val artists = Spotify.Follow.getFollowedArtists(limit = Spotify.MAX_LIMIT)
                .fetchAllCustom { Spotify.get<Spotify.ArtistsCursorPagingModel>(it).artists }
            val savedArtists = GlobalObjects.SavedArtists(ids = artists.map { it.id })
            cache.putAll(artists.plus(savedArtists))

            return savedArtists.ids
        }
    }

    object Playlists {
        suspend fun getPlaylist(id: String): Playlist = cache.get<Playlist>(id) { Spotify.Playlists.getPlaylist(id) }
        suspend fun getFullPlaylist(id: String): FullPlaylist = cache.get(id) { Spotify.Playlists.getPlaylist(id) }

        suspend fun getSavedPlaylists(): List<String> {
            cache.getCachedValue<GlobalObjects.SavedPlaylists>(GlobalObjects.SavedPlaylists.ID)
                ?.let { return it.ids }

            val playlists = Spotify.Playlists.getPlaylists(limit = Spotify.MAX_LIMIT)
                .fetchAll<SimplifiedPlaylist>()
            val savedPlaylists = GlobalObjects.SavedPlaylists(ids = playlists.map { it.id })
            cache.putAll(playlists.plus(savedPlaylists))

            return savedPlaylists.ids
        }
    }

    object Tracks {
        suspend fun getTrack(id: String): Track = cache.get<Track>(id) { Spotify.Tracks.getTrack(id) }
        suspend fun getFullTrack(id: String): FullTrack = cache.get(id) { Spotify.Tracks.getTrack(id) }

        suspend fun getFullTracks(ids: List<String>): List<FullTrack> {
            return cache.getAll(ids = ids) { missingIds ->
                missingIds.chunked(size = Spotify.MAX_LIMIT)
                    .flatMap { idsChunk -> Spotify.Tracks.getTracks(ids = idsChunk) }
            }
        }

        suspend fun getSavedTracks(): List<String> {
            cache.getCachedValue<GlobalObjects.SavedTracks>(GlobalObjects.SavedTracks.ID)
                ?.let { return it.ids }

            val tracks = Spotify.Library.getSavedTracks(limit = Spotify.MAX_LIMIT).fetchAll<SavedTrack>()
            val savedTracks = GlobalObjects.SavedTracks(ids = tracks.map { it.track.id })
            cache.putAll(tracks.plus(savedTracks))

            return savedTracks.ids
        }
    }

    object UsersProfile {
        suspend fun getCurrentUser(): PrivateUser {
            cache.getCachedValue<PrivateUser>(GlobalObjects.CURRENT_USER_ID)?.let { return it }

            val user: PrivateUser = Spotify.UsersProfile.getCurrentUser()

            // cache the user both under its own id and the current id, this way it can be accessed from either
            cache.putAll(mapOf(user.id to user, GlobalObjects.CURRENT_USER_ID to user))

            return user
        }
    }
}
