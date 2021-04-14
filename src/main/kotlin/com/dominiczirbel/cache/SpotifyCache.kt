package com.dominiczirbel.cache

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dominiczirbel.Logger
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.Album
import com.dominiczirbel.network.model.Artist
import com.dominiczirbel.network.model.Episode
import com.dominiczirbel.network.model.FullAlbum
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.network.model.FullEpisode
import com.dominiczirbel.network.model.FullPlaylist
import com.dominiczirbel.network.model.FullShow
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.Playlist
import com.dominiczirbel.network.model.PrivateUser
import com.dominiczirbel.network.model.PublicUser
import com.dominiczirbel.network.model.SavedAlbum
import com.dominiczirbel.network.model.SavedTrack
import com.dominiczirbel.network.model.Show
import com.dominiczirbel.network.model.SimplifiedAlbum
import com.dominiczirbel.network.model.SimplifiedArtist
import com.dominiczirbel.network.model.SimplifiedEpisode
import com.dominiczirbel.network.model.SimplifiedPlaylist
import com.dominiczirbel.network.model.SimplifiedShow
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.model.Track
import com.dominiczirbel.network.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File

object SpotifyCache {
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

    /**
     * The base directory for all cache files.
     */
    val CACHE_DIR by lazy {
        File("cache")
            .also { it.mkdirs() }
            .also { require(it.isDirectory) { "could not create cache directory $it" } }
    }

    private val cacheFile = CACHE_DIR.resolve("cache.json")

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
        suspend fun getAlbum(id: String): Album = cache.get<Album>(id) { Spotify.Albums.getAlbum(id) }
        suspend fun getFullAlbum(id: String): FullAlbum = cache.get(id) { Spotify.Albums.getAlbum(id) }

        suspend fun saveAlbum(id: String) {
            Spotify.Library.saveAlbums(listOf(id))

            cache.getCached(GlobalObjects.SavedAlbums.ID)?.let { albums ->
                val savedAlbums = albums.obj as GlobalObjects.SavedAlbums

                // TODO don't update cache time (or find a different mechanism for last-fetched-from-network-time?)
                cache.put(savedAlbums.copy(ids = savedAlbums.ids.plus(id)))
            }
        }

        suspend fun unsaveAlbum(id: String) {
            Spotify.Library.removeAlbums(listOf(id))

            cache.getCached(GlobalObjects.SavedAlbums.ID)?.let { albums ->
                val savedAlbums = albums.obj as GlobalObjects.SavedAlbums

                // TODO don't update cache time (or find a different mechanism for last-fetched-from-network-time?)
                cache.put(savedAlbums.copy(ids = savedAlbums.ids.minus(id)))
            }
        }

        suspend fun getSavedAlbums(): List<String> {
            return cache.get(GlobalObjects.SavedAlbums.ID) {
                val albums = Spotify.Library.getSavedAlbums(limit = Spotify.MAX_LIMIT).fetchAll<SavedAlbum>()
                cache.putAll(albums)
                GlobalObjects.SavedAlbums(ids = albums.map { it.album.id })
            }.ids
        }
    }

    object Artists {
        suspend fun getArtist(id: String): Artist = cache.get<Artist>(id) { Spotify.Artists.getArtist(id) }
        suspend fun getFullArtist(id: String): FullArtist = cache.get(id) { Spotify.Artists.getArtist(id) }

        suspend fun getArtistAlbums(artistId: String, scope: CoroutineScope): List<Album> {
            val albumIds = cache.get(GlobalObjects.ArtistAlbums.idFor(artistId = artistId)) {
                val albums = Spotify.Artists.getArtistAlbums(id = artistId).fetchAll<SimplifiedAlbum>()
                GlobalObjects.ArtistAlbums(artistId = artistId, albumIds = albums.map { requireNotNull(it.id) })
            }.albumIds

            return cache.getAll<Album>(ids = albumIds) { id -> scope.async { Spotify.Albums.getAlbum(id) } }
        }

        suspend fun getSavedArtists(): List<String> {
            return cache.get(GlobalObjects.SavedArtists.ID) {
                val artists = Spotify.Follow.getFollowedArtists(limit = Spotify.MAX_LIMIT)
                    .fetchAllCustom { Spotify.get<Spotify.ArtistsCursorPagingModel>(it).artists }
                cache.putAll(artists)
                GlobalObjects.SavedArtists(ids = artists.map { it.id })
            }.ids
        }
    }

    object Playlists {
        suspend fun getPlaylist(id: String): Playlist = cache.get<Playlist>(id) { Spotify.Playlists.getPlaylist(id) }
        suspend fun getFullPlaylist(id: String): FullPlaylist = cache.get(id) { Spotify.Playlists.getPlaylist(id) }

        suspend fun getSavedPlaylists(): List<String> {
            return cache.get(GlobalObjects.SavedPlaylists.ID) {
                val playlists = Spotify.Playlists.getPlaylists(limit = Spotify.MAX_LIMIT)
                    .fetchAll<SimplifiedPlaylist>()
                cache.putAll(playlists)
                GlobalObjects.SavedPlaylists(ids = playlists.map { it.id })
            }.ids
        }
    }

    object Tracks {
        suspend fun getTrack(id: String): Track = cache.get<Track>(id) { Spotify.Tracks.getTrack(id) }
        suspend fun getFullTrack(id: String): FullTrack = cache.get(id) { Spotify.Tracks.getTrack(id) }

        suspend fun getFullTracks(ids: List<String>, scope: CoroutineScope): List<FullTrack> {
            // TODO batch in getTracks()
            return cache.getAll(ids = ids) { id ->
                scope.async { Spotify.Tracks.getTrack(id = id) }
            }
        }

        suspend fun getSavedTracks(): List<String> {
            return cache.get(GlobalObjects.SavedTracks.ID) {
                val tracks = Spotify.Library.getSavedTracks(limit = Spotify.MAX_LIMIT).fetchAll<SavedTrack>()
                cache.putAll(tracks)
                GlobalObjects.SavedTracks(ids = tracks.map { it.track.id })
            }.ids
        }
    }

    object UsersProfile {
        suspend fun getCurrentUser(): PrivateUser {
            return cache.get(GlobalObjects.CURRENT_USER_ID) {
                // caches the user object twice: once under CURRENT_USER_ID, once under its own ID
                // this is mostly harmless and allows lookups by either ID
                Spotify.UsersProfile.getCurrentUser().also { cache.put(GlobalObjects.CURRENT_USER_ID, it) }
            }
        }
    }
}
