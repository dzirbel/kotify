package com.dominiczirbel.cache

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
import kotlinx.serialization.Serializable
import java.io.File

object SpotifyCache {
    @Serializable
    private data class Library(
        val albums: List<String>? = null,
        val artists: List<String>? = null,
        val tracks: List<String>? = null
    )

    private val cache = Cache(
        file = File("cache.json"),

        saveOnChange = true,

        ttlStrategy = Cache.TTLStrategy.AlwaysValid,

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

    private var library: Library
        get() = cache.getCached(LIBRARY_KEY)?.obj as? Library ?: Library()
        set(value) {
            cache.put(LIBRARY_KEY, value)
        }

    /**
     * Loads the cache from disk, overwriting any values currently in memory.
     */
    fun load() {
        cache.load()
    }

    /**
     * A convenience function which applies [update] to [Library]; this primarily exists to conveniently avoid calling
     * the [library] getter twice.
     */
    private fun updateLibrary(update: Library.() -> Library) {
        library = update(library)
    }

    object Albums {
        suspend fun getAlbum(id: String): Album = cache.get<Album>(id) { Spotify.Albums.getAlbum(id) }
        suspend fun getFullAlbum(id: String): FullAlbum = cache.get(id) { Spotify.Albums.getAlbum(id) }

        suspend fun saveAlbum(id: String) {
            Spotify.Library.saveAlbums(listOf(id))
                .also { updateLibrary { copy(albums = albums?.plus(id)) } }
        }

        suspend fun unsaveAlbum(id: String) {
            Spotify.Library.removeAlbums(listOf(id))
                .also { updateLibrary { copy(albums = albums?.minus(id)) } }
        }

        suspend fun getSavedAlbums(): List<String> {
            return library.albums
                ?: Spotify.Library.getSavedAlbums(limit = Spotify.MAX_LIMIT)
                    .fetchAll<SavedAlbum>()
                    .map { it.album }
                    .also { cache.putAll(it) }
                    .map { it.id }
                    .also { albums -> updateLibrary { copy(albums = albums) } }
        }
    }

    object Artists {
        suspend fun getArtist(id: String): Artist = cache.get<Artist>(id) { Spotify.Artists.getArtist(id) }
        suspend fun getFullArtist(id: String): FullArtist = cache.get(id) { Spotify.Artists.getArtist(id) }

        suspend fun getSavedArtists(): List<String> {
            return library.artists
                ?: Spotify.Follow.getFollowedArtists(limit = Spotify.MAX_LIMIT)
                    .fetchAllCustom { Spotify.get<Spotify.ArtistsCursorPagingModel>(it).artists }
                    .also { cache.putAll(it) }
                    .map { it.id }
                    .also { artists -> updateLibrary { copy(artists = artists) } }
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
                    .also { cache.putAll(it) }
                    .map { it.id }
                    .also { tracks -> updateLibrary { copy(tracks = tracks) } }
        }
    }
}
