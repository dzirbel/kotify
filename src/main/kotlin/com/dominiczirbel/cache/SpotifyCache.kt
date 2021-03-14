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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File

object SpotifyCache {
    @Serializable
    private data class Library(
        val currentUser: String? = null,
        val albums: List<String>? = null,
        val artists: List<String>? = null,
        val tracks: List<String>? = null,
        val artistAlbumMap: Map<String, List<String>> = emptyMap()
    ) : CacheableObject {
        override val id = LIBRARY_KEY
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

        ttlStrategy = Cache.TTLStrategy.AlwaysValid,

        eventHandler = Logger.Cache::handleCacheEvents,

        onSave = ::onSave,

        // TODO handle case where simplified object has been updated, but full is now out of date
        replacementStrategy = object : Cache.ReplacementStrategy {
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

    private const val LIBRARY_KEY = "spotify-cache-library"

    // TODO store local copy to avoid hitting the cache
    private val library: Library
        get() = cache.getCached(LIBRARY_KEY)?.obj as? Library ?: Library()

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

    object Albums {
        suspend fun getAlbum(id: String): Album = cache.get<Album>(id) { Spotify.Albums.getAlbum(id) }
        suspend fun getFullAlbum(id: String): FullAlbum = cache.get(id) { Spotify.Albums.getAlbum(id) }

        suspend fun saveAlbum(id: String) {
            Spotify.Library.saveAlbums(listOf(id))
                .also {
                    val library = library
                    cache.put(library.copy(albums = library.albums?.plus(id)))
                }
        }

        suspend fun unsaveAlbum(id: String) {
            Spotify.Library.removeAlbums(listOf(id))
                .also {
                    val library = library
                    cache.put(library.copy(albums = library.albums?.minus(id)))
                }
        }

        suspend fun getSavedAlbums(): List<String> {
            return library.albums
                ?: Spotify.Library.getSavedAlbums(limit = Spotify.MAX_LIMIT)
                    .fetchAll<SavedAlbum>()
                    .map { it.album }
                    .let { albums ->
                        val albumIds = albums.map { it.id }
                        cache.putAll(albums.plus(library.copy(albums = albumIds)))

                        albumIds
                    }
        }
    }

    object Artists {
        suspend fun getArtist(id: String): Artist = cache.get<Artist>(id) { Spotify.Artists.getArtist(id) }
        suspend fun getFullArtist(id: String): FullArtist = cache.get(id) { Spotify.Artists.getArtist(id) }

        suspend fun getArtistAlbums(artistId: String): List<Album> {
            return library.artistAlbumMap[artistId]
                ?.map { Albums.getAlbum(it) }
                ?: Spotify.Artists.getArtistAlbums(id = artistId)
                    .fetchAll<SimplifiedAlbum>()
                    .also { albums ->
                        val albumIds = albums.map { requireNotNull(it.id) }
                        val library = library
                        val artistAlbumMap = library.artistAlbumMap.plus(artistId to albumIds)
                        cache.putAll(albums.plus(library.copy(artistAlbumMap = artistAlbumMap)))
                    }
        }

        suspend fun getSavedArtists(): List<String> {
            return library.artists
                ?: Spotify.Follow.getFollowedArtists(limit = Spotify.MAX_LIMIT)
                    .fetchAllCustom { Spotify.get<Spotify.ArtistsCursorPagingModel>(it).artists }
                    .let { artists ->
                        val artistIds = artists.map { it.id }
                        cache.putAll(artists.plus(library.copy(artists = artistIds)))

                        artistIds
                    }
        }
    }

    object Tracks {
        suspend fun getTrack(id: String): Track = cache.get<Track>(id) { Spotify.Tracks.getTrack(id) }
        suspend fun getFullTrack(id: String): FullTrack = cache.get(id) { Spotify.Tracks.getTrack(id) }

        suspend fun getSavedTracks(): List<String> {
            return library.tracks
                ?: Spotify.Library.getSavedTracks(limit = Spotify.MAX_LIMIT)
                    .fetchAll<SavedTrack>()
                    .map { it.track }
                    .let { tracks ->
                        val trackIds = tracks.map { it.id }
                        cache.putAll(tracks.plus(library.copy(tracks = trackIds)))

                        trackIds
                    }
        }
    }

    object UsersProfile {
        suspend fun getCurrentUser(): PrivateUser {
            val id = library.currentUser
                ?: Spotify.UsersProfile.getCurrentUser()
                    .also { user ->
                        cache.putAll(
                            listOf(user, library.copy(currentUser = user.id))
                        )
                    }
                    .id

            return cache.get(id) {
                Spotify.UsersProfile.getCurrentUser()
                    .also { user ->
                        val library = library
                        if (user.id != library.currentUser) {
                            cache.put(library.copy(currentUser = user.id))
                        }
                    }
            }
        }
    }
}
