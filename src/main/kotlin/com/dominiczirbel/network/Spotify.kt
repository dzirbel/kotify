package com.dominiczirbel.network

import com.dominiczirbel.network.model.Album
import com.dominiczirbel.network.model.AudioAnalysis
import com.dominiczirbel.network.model.AudioFeatures
import com.dominiczirbel.network.model.Category
import com.dominiczirbel.network.model.CursorPaging
import com.dominiczirbel.network.model.FullAlbum
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.network.model.FullEpisode
import com.dominiczirbel.network.model.FullPlaylist
import com.dominiczirbel.network.model.FullShow
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.Image
import com.dominiczirbel.network.model.Paging
import com.dominiczirbel.network.model.PlaylistTrack
import com.dominiczirbel.network.model.PrivateUser
import com.dominiczirbel.network.model.PublicUser
import com.dominiczirbel.network.model.Recommendations
import com.dominiczirbel.network.model.SimplifiedAlbum
import com.dominiczirbel.network.model.SimplifiedEpisode
import com.dominiczirbel.network.model.SimplifiedPlaylist
import com.dominiczirbel.network.model.SimplifiedShow
import com.dominiczirbel.network.model.SimplifiedTrack
import com.dominiczirbel.network.oauth.AccessToken
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.dzirbel.gson.bijectivereflection.BijectiveReflectiveTypeAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/
 * https://developer.spotify.com/documentation/web-api/reference-beta
 */
object Spotify {
    data class Configuration(
        val okHttpClient: OkHttpClient = OkHttpClient(),
        val oauthOkHttpClient: OkHttpClient = OkHttpClient()
    )

    val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapterFactory(BijectiveReflectiveTypeAdapterFactory()) // TODO maybe only for tests/debug builds
        .create()

    var configuration: Configuration = Configuration()

    const val FROM_TOKEN = "from_token"
    const val API_URL = "https://api.spotify.com/v1/"

    class SpotifyError(val code: Int, message: String) : Throwable(message = "HTTP $code : $message")

    data class ErrorObject(val error: ErrorDetails)
    data class ErrorDetails(val status: Int, val message: String)

    private data class AlbumsModel(val albums: List<FullAlbum>)
    private data class AlbumsPagingModel(val albums: Paging<SimplifiedAlbum>)
    private data class ArtistsModel(val artists: List<FullArtist>)
    private data class ArtistsCursorPagingModel(val artists: CursorPaging<FullArtist>)
    private data class AudioFeaturesModel(val audioFeatures: List<AudioFeatures>)
    private data class CategoriesModel(val categories: Paging<Category>)
    private data class EpisodesModel(val episodes: List<FullEpisode>)
    private data class PlaylistPagingModel(val playlists: Paging<SimplifiedPlaylist>, val message: String?)
    private data class ShowsModel(val shows: List<SimplifiedShow>)
    private data class TracksModel(val tracks: List<FullTrack>)

    suspend inline fun <reified T : Any> get(path: String, queryParams: List<Pair<String, String?>>? = null): T {
        val token = AccessToken.Cache.getOrThrow()

        val url = (API_URL + path).toHttpUrl()
            .newBuilder()
            .apply {
                queryParams?.forEach { (key, value) ->
                    value?.let { addQueryParameter(key, it) }
                }
            }
            .build()

        val request = Request.Builder()
            .get()
            .url(url)
            .header("Authorization", "${token.tokenType} ${token.accessToken}")
            .build()

        return configuration.okHttpClient.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                val message = runCatching { response.bodyFromJson<ErrorObject>(gson) }
                    .getOrNull()
                    ?.error
                    ?.message
                    ?: response.message
                throw SpotifyError(code = response.code, message = message)
            }

            response.bodyFromJson(gson)
        }
    }

    /**
     * Endpoints for retrieving information about one or more albums from the Spotify catalog.
     *
     * https://developer.spotify.com/documentation/web-api/reference/albums/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-albums
     */
    object Albums {
        /**
         * Get Spotify catalog information for a single album.
         *
         * https://developer.spotify.com/documentation/web-api/reference/albums/get-album/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-album
         *
         * @param id The Spotify ID for the album.
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking.
         */
        suspend fun getAlbum(id: String, market: String? = null): FullAlbum {
            return get("albums/$id", listOf("market" to market))
        }

        /**
         * Get Spotify catalog information about an album’s tracks. Optional parameters can be used to limit the number
         * of tracks returned.
         *
         * https://developer.spotify.com/documentation/web-api/reference/albums/get-albums-tracks/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-albums-tracks
         *
         * @param id The Spotify ID for the album.
         * @param limit Optional. The maximum number of tracks to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first track to return. Default: 0 (the first object). Use with limit
         *  to get the next set of tracks.
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking.
         */
        suspend fun getAlbumTracks(
            id: String,
            limit: Int? = null,
            offset: Int? = null,
            market: String? = null
        ): Paging<SimplifiedTrack> {
            return get(
                "albums/$id/tracks",
                listOf("limit" to limit?.toString(), "offset" to offset?.toString(), "market" to market)
            )
        }

        /**
         * Get Spotify catalog information for multiple albums identified by their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/albums/get-several-albums/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-multiple-albums
         *
         * @param ids Required. A comma-separated list of the Spotify IDs for the albums. Maximum: 20 IDs.
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking.
         */
        suspend fun getAlbums(ids: List<String>, market: String? = null): List<FullAlbum> {
            return get<AlbumsModel>(
                "albums",
                listOf("ids" to ids.joinToString(separator = ","), "market" to market)
            ).albums
        }
    }

    /**
     * Endpoints for retrieving information about one or more artists from the Spotify catalog.
     *
     * https://developer.spotify.com/documentation/web-api/reference/artists/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-artists
     */
    object Artists {
        /**
         * Get Spotify catalog information for a single artist identified by their unique Spotify ID.
         *
         * https://developer.spotify.com/documentation/web-api/reference/artists/get-artist/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-artist
         *
         * @param id The Spotify ID for the artist.
         */
        suspend fun getArtist(id: String): FullArtist = get("artists/$id")

        /**
         * Get Spotify catalog information for several artists based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/artists/get-several-artists/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-multiple-artists
         *
         * @param ids A comma-separated list of the Spotify IDs for the artists. Maximum: 50 IDs.
         */
        suspend fun getArtists(ids: List<String>): List<FullArtist> {
            return get<ArtistsModel>("artists", listOf("ids" to ids.joinToString(separator = ","))).artists
        }

        /**
         * Get Spotify catalog information about an artist’s albums.
         *
         * https://developer.spotify.com/documentation/web-api/reference/artists/get-artists-albums/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-artists-albums
         *
         * @param id The Spotify ID for the artist.
         * @param includeGroups Optional. A comma-separated list of keywords that will be used to filter the response.
         *  If not supplied, all album types will be returned. Valid values are:
         *  - album
         *  - single
         *  - appears_on
         *  - compilation
         *  For example: include_groups=album,single.
         * @param country Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Supply this parameter
         *  to limit the response to one particular geographical market. For example, for albums available in Sweden:
         *  country=SE. If not given, results will be returned for all countries and you are likely to get duplicate
         *  results per album, one for each country in which the album is available!
         * @param limit The number of album objects to return. Default: 20. Minimum: 1. Maximum: 50. For example:
         *  limit=2
         * @Param offset The index of the first album to return. Default: 0 (i.e., the first album). Use with limit to
         *  get the next set of albums.
         */
        suspend fun getArtistAlbums(
            id: String,
            includeGroups: List<Album.Type>? = null,
            country: String? = null,
            limit: Int? = null,
            offset: Int? = null
        ): Paging<SimplifiedAlbum> {
            return get(
                "artists/$id/albums",
                listOf(
                    "include_groups" to includeGroups?.joinToString(separator = ",") { it.name.toLowerCase(Locale.US) },
                    "country" to country,
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString()
                )
            )
        }

        /**
         * Get Spotify catalog information about an artist’s top tracks by country.
         *
         * https://developer.spotify.com/documentation/web-api/reference/artists/get-artists-top-tracks/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-artists-top-tracks
         *
         * @param id The Spotify ID for the artist
         * @param country Required. An ISO 3166-1 alpha-2 country code or the string from_token.
         */
        suspend fun getArtistTopTracks(id: String, country: String): List<FullTrack> {
            return get<TracksModel>("artists/$id/top-tracks", listOf("country" to country)).tracks
        }

        /**
         * Get Spotify catalog information about artists similar to a given artist. Similarity is based on analysis of
         * the Spotify community’s listening history.
         *
         * https://developer.spotify.com/documentation/web-api/reference/artists/get-related-artists/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-artists-top-tracks
         *
         * @param id The Spotify ID for the artist
         */
        suspend fun getArtistRelatedArtists(id: String): List<FullArtist> {
            return get<ArtistsModel>("artists/$id/related-artists").artists
        }
    }

    /**
     * Endpoints for getting playlists and new album releases featured on Spotify’s Browse tab.
     *
     * https://developer.spotify.com/documentation/web-api/reference/browse/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-browse
     */
    object Browse {
        /**
         * Get a single category used to tag items in Spotify (on, for example, the Spotify player’s "Browse" tab).
         *
         * https://developer.spotify.com/documentation/web-api/reference/browse/get-category/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-a-category
         *
         * @param categoryId The Spotify category ID for the category.
         * @param country Optional. A country: an ISO 3166-1 alpha-2 country code. Provide this parameter to ensure that
         *  the category exists for a particular country.
         * @param locale Optional. The desired language, consisting of an ISO 639-1 language code and an ISO 3166-1
         *  alpha-2 country code, joined by an underscore. For example: es_MX, meaning "Spanish (Mexico)". Provide this
         *  parameter if you want the category strings returned in a particular language. Note that, if locale is not
         *  supplied, or if the specified language is not available, the category strings returned will be in the
         *  Spotify default language (American English).
         */
        suspend fun getCategory(categoryId: String, country: String? = null, locale: String? = null): Category {
            return get("browse/categories/$categoryId", listOf("country" to country, "locale" to locale))
        }

        /**
         * Get a list of Spotify playlists tagged with a particular category.
         *
         * https://developer.spotify.com/documentation/web-api/reference/browse/get-categorys-playlists/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-a-categories-playlists
         *
         * @param categoryId The Spotify category ID for the category.
         * @param country Optional. A country: an ISO 3166-1 alpha-2 country code.
         * @param limit Optional. The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first item to return. Default: 0 (the first object). Use with limit
         *  to get the next set of items.
         */
        suspend fun getCategoryPlaylists(
            categoryId: String,
            country: String? = null,
            limit: Int? = null,
            offset: Int? = null
        ): Paging<SimplifiedPlaylist> {
            return get<PlaylistPagingModel>(
                "browse/categories/$categoryId/playlists",
                listOf("country" to country, "limit" to limit?.toString(), "offset" to offset?.toString())
            ).playlists
        }

        /**
         * Get a list of categories used to tag items in Spotify (on, for example, the Spotify player’s “Browse” tab).
         *
         * https://developer.spotify.com/documentation/web-api/reference/browse/get-list-categories/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-categories
         *
         * @param country Optional. A country: an ISO 3166-1 alpha-2 country code. Provide this parameter if you want to
         *  narrow the list of returned categories to those relevant to a particular country. If omitted, the returned
         *  items will be globally relevant.
         * @param locale Optional. The desired language, consisting of an ISO 639-1 language code and an ISO 3166-1
         *  alpha-2 country code, joined by an underscore. For example: es_MX, meaning “Spanish (Mexico)”. Provide this
         *  parameter if you want the category metadata returned in a particular language. Note that, if locale is not
         *  supplied, or if the specified language is not available, all strings will be returned in the Spotify default
         *  language (American English). The locale parameter, combined with the country parameter, may give odd results
         *  if not carefully matched. For example country=SE&locale=de_DE will return a list of categories relevant to
         *  Sweden but as German language strings.
         * @param limit Optional. The maximum number of categories to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first item to return. Default: 0 (the first object). Use with limit
         *  to get the next set of categories.
         */
        suspend fun getCategories(
            country: String? = null,
            locale: String? = null,
            limit: Int? = null,
            offset: Int? = null
        ): Paging<Category> {
            return get<CategoriesModel>(
                "browse/categories",
                listOf(
                    "country" to country,
                    "locale" to locale,
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString()
                )
            ).categories
        }

        /**
         * Get a list of Spotify featured playlists (shown, for example, on a Spotify player's 'Browse' tab).
         *
         * https://developer.spotify.com/documentation/web-api/reference/browse/get-list-featured-playlists/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-featured-playlists
         *
         * @param locale Optional. The desired language, consisting of a lowercase ISO 639-1 language code and an
         *  uppercase ISO 3166-1 alpha-2 country code, joined by an underscore. For example: es_MX, meaning "Spanish
         *  (Mexico)". Provide this parameter if you want the results returned in a particular language (where
         *  available). Note that, if locale is not supplied, or if the specified language is not available, all strings
         *  will be returned in the Spotify default language (American English). The locale parameter, combined with the
         *  country parameter, may give odd results if not carefully matched. For example country=SE&locale=de_DE will
         *  return a list of categories relevant to Sweden but as German language strings.
         * @param country Optional. A country: an ISO 3166-1 alpha-2 country code. Provide this parameter if you want
         *  the list of returned items to be relevant to a particular country. If omitted, the returned items will be
         *  relevant to all countries.
         * @param timestamp Optional. A timestamp in ISO 8601 format: yyyy-MM-ddTHH:mm:ss. Use this parameter to specify
         *  the user’s local time to get results tailored for that specific date and time in the day. If not provided,
         *  the response defaults to the current UTC time. Example: "2014-10-23T09:00:00" for a user whose local time is
         *  9AM. If there were no featured playlists (or there is no data) at the specified time, the response will
         *  revert to the current UTC time.
         * @param limit Optional. The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first item to return. Default: 0 (the first object). Use with limit
         *  to get the next set of items.
         */
        suspend fun getFeaturedPlaylists(
            locale: String? = null,
            country: String? = null,
            timestamp: String? = null,
            limit: Int? = null,
            offset: Int? = null
        ): Paging<SimplifiedPlaylist> {
            return get<PlaylistPagingModel>(
                "browse/featured-playlists",
                listOf(
                    "locale" to locale,
                    "country" to country,
                    "timestamp" to timestamp,
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString()
                )
            ).playlists
        }

        /**
         * Get a list of new album releases featured in Spotify (shown, for example, on a Spotify player's "Browse"
         * tab).
         *
         * https://developer.spotify.com/documentation/web-api/reference/browse/get-list-new-releases/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-new-releases
         *
         * @param country Optional. A country: an ISO 3166-1 alpha-2 country code. Provide this parameter if you want
         *  the list of returned items to be relevant to a particular country. If omitted, the returned items will be
         *  relevant to all countries.
         * @param limit Optional. The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first item to return. Default: 0 (the first object). Use with limit
         *  to get the next set of items.
         */
        suspend fun getNewReleases(
            country: String? = null,
            limit: Int? = null,
            offset: Int? = null
        ): Paging<SimplifiedAlbum> {
            return get<AlbumsPagingModel>(
                "browse/new-releases",
                listOf("country" to country, "limit" to limit?.toString(), "offset" to offset?.toString())
            ).albums
        }

        /**
         * Create a playlist-style listening experience based on seed artists, tracks and genres.
         *
         * Recommendations are generated based on the available information for a given seed entity and matched against
         * similar artists and tracks. If there is sufficient information about the provided seeds, a list of tracks
         * will be returned together with pool size details.
         *
         * For artists and tracks that are very new or obscure there might not be enough data to generate a list of
         * tracks.
         *
         * https://developer.spotify.com/documentation/web-api/reference/browse/get-recommendations/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-recommendations
         *
         * @param limit Optional. The target size of the list of recommended tracks. For seeds with unusually small
         *  pools or when highly restrictive filtering is applied, it may be impossible to generate the requested number
         *  of recommended tracks. Debugging information for such cases is available in the response. Default: 20.
         *  Minimum: 1. Maximum: 100.
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking. Because min_*, max_* and target_* are applied to pools before
         *  relinking, the generated results may not precisely match the filters applied. Original, non-relinked tracks
         *  are available via the linked_from attribute of the relinked track response.
         * @param seedArtists A comma separated list of Spotify IDs for seed artists. Up to 5 seed values may be
         *  provided in any combination of seed_artists, seed_tracks and seed_genres.
         * @param seedGenres A comma separated list of any genres in the set of available genre seeds. Up to 5 seed
         *  values may be provided in any combination of seed_artists, seed_tracks and seed_genres.
         * @param seedTracks A comma separated list of Spotify IDs for a seed track. Up to 5 seed values may be provided
         *  in any combination of seed_artists, seed_tracks and seed_genres.
         * @param tunableTrackAttributes A set maximums, minimums, and targets for tunable track attributes. See the
         *  Spotify documentation for details.
         */
        suspend fun getRecommendations(
            limit: Int? = null,
            market: String? = null,
            seedArtists: List<String>,
            seedGenres: List<String>,
            seedTracks: List<String>,
            tunableTrackAttributes: Map<String, String> = emptyMap()
        ): Recommendations {
            return get(
                "recommendations",
                listOf(
                    "limit" to limit?.toString(),
                    "market" to market,
                    "seed_artists" to seedArtists.joinToString(separator = ","),
                    "seed_genres" to seedGenres.joinToString(separator = ","),
                    "seed_tracks" to seedTracks.joinToString(separator = ",")
                ).plus(tunableTrackAttributes.toList())
            )
        }
    }

    /**
     * Endpoints for retrieving information about one or more episodes from the Spotify catalog.
     *
     * https://developer.spotify.com/documentation/web-api/reference/episodes/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-episodes
     */
    object Episodes {
        /**
         * Get Spotify catalog information for a single episode identified by its unique Spotify ID.
         *
         * https://developer.spotify.com/documentation/web-api/reference/episodes/get-an-episode/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-episode
         *
         * @param id The Spotify ID for the episode.
         * @param market Optional. An ISO 3166-1 alpha-2 country code. If a country code is specified, only shows and
         *  episodes that are available in that market will be returned. If a valid user access token is specified in
         *  the request header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the content is considered unavailable for the client.
         *  Users can view the country that is associated with their account in the account settings.
         */
        suspend fun getEpisode(id: String, market: String? = null): FullEpisode {
            return get("episodes/$id", listOf("market" to market))
        }

        /**
         * Get Spotify catalog information for multiple episodes based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/episodes/get-several-episodes/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-multiple-episodes
         *
         * @param ids Required. A comma-separated list of the Spotify IDs for the episodes. Maximum: 50 IDs.
         * @param market Optional. An ISO 3166-1 alpha-2 country code. If a country code is specified, only shows and
         *  episodes that are available in that market will be returned. If a valid user access token is specified in
         *  the request header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the content is considered unavailable for the client.
         *  Users can view the country that is associated with their account in the account settings.
         */
        suspend fun getEpisodes(ids: List<String>, market: String? = null): List<FullEpisode> {
            return get<EpisodesModel>(
                "episodes",
                listOf("ids" to ids.joinToString(separator = ","), "market" to market)
            ).episodes
        }
    }

    /**
     * Endpoints for managing the artists, users, and playlists that a Spotify user follows.
     *
     * https://developer.spotify.com/documentation/web-api/reference/follow/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-follow
     *
     * TODO PUT https://developer.spotify.com/documentation/web-api/reference/follow/follow-artists-users/
     * TODO PUT https://developer.spotify.com/documentation/web-api/reference/follow/follow-playlist/
     * TODO DELETE https://developer.spotify.com/documentation/web-api/reference/follow/unfollow-artists-users/
     * TODO DELETE https://developer.spotify.com/documentation/web-api/reference/follow/unfollow-playlist/
     */
    object Follow {
        /**
         * Check to see if the current user is following one or more artists or other Spotify users.
         *
         * https://developer.spotify.com/documentation/web-api/reference/follow/check-current-user-follows/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-check-current-user-follows
         *
         * @param type Required. The ID type: either artist or user.
         * @param ids Required. A comma-separated list of the artist or the user Spotify IDs to check. For example:
         *  ids=74ASZWbe4lXaubB36ztrGX,08td7MxkoHQkXnWAYD8d6Q. A maximum of 50 IDs can be sent in one request.
         */
        suspend fun isFollowing(type: String, ids: List<String>): List<Boolean> {
            return get("me/following/contains", listOf("type" to type, "ids" to ids.joinToString(separator = ",")))
        }

        /**
         * Check to see if one or more Spotify users are following a specified playlist.
         *
         * https://developer.spotify.com/documentation/web-api/reference/follow/check-user-following-playlist/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-check-if-user-follows-playlist
         *
         * @param playlistId The Spotify ID of the playlist.
         * @param userIds Required. A comma-separated list of Spotify User IDs ; the ids of the users that you want to
         *  check to see if they follow the playlist. Maximum: 5 ids.
         */
        suspend fun isFollowingPlaylist(playlistId: String, userIds: List<String>): List<Boolean> {
            return get(
                "playlists/$playlistId/followers/contains",
                listOf("ids" to userIds.joinToString(separator = ","))
            )
        }

        /**
         * Get the current user’s followed artists.
         *
         * https://developer.spotify.com/documentation/web-api/reference/follow/get-followed/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-followed
         *
         * @param limit Optional. The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param after Optional. The last artist ID retrieved from the previous request.
         */
        suspend fun getFollowedArtists(limit: Int? = null, after: String? = null): CursorPaging<FullArtist> {
            return get<ArtistsCursorPagingModel>(
                "me/following",
                listOf("type" to "artist", "limit" to limit?.toString(), "after" to after)
            ).artists
        }
    }

    /**
     * Endpoints for retrieving information about, and managing, tracks that the current user has saved in their "Your
     * Music" library.
     *
     * https://developer.spotify.com/documentation/web-api/reference/library/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-library
     */
    object Library {
        // TODO add library endpoints
    }

    /**
     * Endpoints for retrieving information about the user's listening habits.
     *
     * https://developer.spotify.com/documentation/web-api/reference/personalization/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-personalization
     */
    object Personalization {
        enum class TimeRange(val value: String) {
            SHORT_TERM("short_term"),
            MEDIUM_TERM("medium_term"),
            LONG_TERM("long_term")
        }

        /**
         * Get the current user’s top artists or tracks based on calculated affinity.
         *
         * Affinity is a measure of the expected preference a user has for a particular track or artist. It is based on
         * user behavior, including play history, but does not include actions made while in incognito mode. Light or
         * infrequent users of Spotify may not have sufficient play history to generate a full affinity data set. As a
         * user's behavior is likely to shift over time, this preference data is available over three time spans. See
         * time_range in the query parameter table for more information. For each time range, the top 50 tracks and
         * artists are available for each user. In the future, it is likely that this restriction will be relaxed.
         * This data is typically updated once each day for each user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/personalization/get-users-top-artists-and-tracks/
         *
         * @Param limit Optional. The number of entities to return. Default: 20. Minimum: 1. Maximum: 50. For example:
         *  limit=2
         * @param offset Optional. The index of the first entity to return. Default: 0 (i.e., the first track). Use with
         *  limit to get the next set of entities.
         * @param timeRange Optional. Over what time frame the affinities are computed. Valid values: long_term
         *  (calculated from several years of data and including all new data as it becomes available), medium_term
         *  (approximately last 6 months), short_term (approximately last 4 weeks). Default: medium_term.
         */
        suspend fun topArtists(
            limit: Int? = null,
            offset: Int? = null,
            timeRange: TimeRange? = null
        ): Paging<FullArtist> {
            return get(
                "me/top/artists",
                listOf("limit" to limit?.toString(), "offset" to offset?.toString(), "time_range" to timeRange?.value)
            )
        }

        /**
         * Get the current user’s top artists or tracks based on calculated affinity.
         *
         * Affinity is a measure of the expected preference a user has for a particular track or artist. It is based on
         * user behavior, including play history, but does not include actions made while in incognito mode. Light or
         * infrequent users of Spotify may not have sufficient play history to generate a full affinity data set. As a
         * user's behavior is likely to shift over time, this preference data is available over three time spans. See
         * time_range in the query parameter table for more information. For each time range, the top 50 tracks and
         * artists are available for each user. In the future, it is likely that this restriction will be relaxed.
         * This data is typically updated once each day for each user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/personalization/get-users-top-artists-and-tracks/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-users-top-artists-and-tracks
         *
         * @Param limit Optional. The number of entities to return. Default: 20. Minimum: 1. Maximum: 50. For example:
         *  limit=2
         * @param offset Optional. The index of the first entity to return. Default: 0 (i.e., the first track). Use with
         *  limit to get the next set of entities.
         * @param timeRange Optional. Over what time frame the affinities are computed. Valid values: long_term
         *  (calculated from several years of data and including all new data as it becomes available), medium_term
         *  (approximately last 6 months), short_term (approximately last 4 weeks). Default: medium_term.
         */
        suspend fun topTracks(
            limit: Int? = null,
            offset: Int? = null,
            timeRange: TimeRange? = null
        ): Paging<FullTrack> {
            return get(
                "me/top/tracks",
                listOf("limit" to limit?.toString(), "offset" to offset?.toString(), "time_range" to timeRange?.value)
            )
        }
    }

    /**
     * These endpoints are in beta. While we encourage you to build with them, a situation may arise where we need to
     * disable some or all of the functionality and/or change how they work without prior notice. Please report any
     * issues via our developer community forum.
     *
     * https://developer.spotify.com/documentation/web-api/reference/player/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-player
     */
    object Player {
        // TODO add player endpoints
    }

    /**
     * Endpoints for retrieving information about a user’s playlists and for managing a user’s playlists.
     *
     * https://developer.spotify.com/documentation/web-api/reference/playlists/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-playlists
     *
     * TODO POST https://developer.spotify.com/documentation/web-api/reference/playlists/add-tracks-to-playlist/
     * TODO PUT https://developer.spotify.com/documentation/web-api/reference/playlists/change-playlist-details/
     * TODO POST https://developer.spotify.com/documentation/web-api/reference/playlists/create-playlist/
     * TODO DELETE https://developer.spotify.com/documentation/web-api/reference/playlists/remove-tracks-playlist/
     * TODO PUT https://developer.spotify.com/documentation/web-api/reference/playlists/reorder-playlists-tracks/
     * TODO PUT https://developer.spotify.com/documentation/web-api/reference/playlists/replace-playlists-tracks/
     * TODO PUT https://developer.spotify.com/documentation/web-api/reference/playlists/upload-custom-playlist-cover/
     */
    object Playlists {
        /**
         * Get a list of the playlists owned or followed by the current Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/playlists/get-a-list-of-current-users-playlists/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-a-list-of-current-users-playlists
         *
         * @param limit Optional. The maximum number of playlists to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first playlist to return. Default: 0 (the first object). Maximum
         *  offset: 100.000. Use with limit to get the next set of playlists.
         */
        suspend fun getPlaylists(limit: Int? = null, offset: Int? = null): Paging<SimplifiedPlaylist> {
            return get("me/playlists", listOf("limit" to limit?.toString(), "offset" to offset?.toString()))
        }

        /**
         * Get a list of the playlists owned or followed by a Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/playlists/get-list-users-playlists/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-list-users-playlists
         *
         * @param limit Optional. The maximum number of playlists to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first playlist to return. Default: 0 (the first object). Maximum
         *  offset: 100.000. Use with limit to get the next set of playlists.
         */
        suspend fun getPlaylists(userId: String, limit: Int? = null, offset: Int? = null): Paging<SimplifiedPlaylist> {
            return get("users/$userId/playlists", listOf("limit" to limit?.toString(), "offset" to offset?.toString()))
        }

        /**
         * Get the current image associated with a specific playlist.
         *
         * https://developer.spotify.com/documentation/web-api/reference/playlists/get-playlist-cover/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-playlist-cover
         *
         * @param playlistId The Spotify ID for the playlist.
         */
        suspend fun getPlaylistCoverImages(playlistId: String): List<Image> {
            return get("playlists/$playlistId/images")
        }

        /**
         * Get a playlist owned by a Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/playlists/get-playlist/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-playlist
         *
         * @param playlistId The Spotify ID for the playlist.
         * @param fields Optional. Filters for the query: a comma-separated list of the fields to return. If omitted,
         *  all fields are returned. For example, to get just the playlist’s description and URI:
         *  fields=description,uri. A dot separator can be used to specify non-reoccurring fields, while parentheses can
         *  be used to specify reoccurring fields within objects. For example, to get just the added date and user ID of
         *  the adder: fields=tracks.items(added_at,added_by.id). Use multiple parentheses to drill down into nested
         *  objects, for example: fields=tracks.items(track(name,href,album(name,href))). Fields can be excluded by
         *  prefixing them with an exclamation mark, for example:
         *  fields=tracks.items(track(name,href,album(!name,href)))
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking. For episodes, if a valid user access token is specified in the request
         *  header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the episode is considered unavailable for the client.
         * @param additionalTypes Optional. A comma-separated list of item types that your client supports besides the
         *  default track type. Valid types are: track and episode. Note: This parameter was introduced to allow
         *  existing clients to maintain their current behaviour and might be deprecated in the future. In addition to
         *  providing this parameter, make sure that your client properly handles cases of new types in the future by
         *  checking against the type field of each object.
         */
        suspend fun getPlaylist(
            playlistId: String,
            fields: List<String>? = null,
            market: String? = null,
            additionalTypes: List<String>? = null
        ): FullPlaylist {
            return get(
                "playlists/$playlistId",
                listOf(
                    "fields" to fields?.joinToString(separator = ","),
                    "market" to market,
                    "additional_types" to additionalTypes?.joinToString(separator = ",")
                )
            )
        }

        /**
         * Get full details of the tracks or episodes of a playlist owned by a Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/playlists/get-playlists-tracks/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-playlists-tracks
         *
         * @param fields Optional. Filters for the query: a comma-separated list of the fields to return. If omitted,
         *  all fields are returned. For example, to get just the total number of tracks and the request limit:
         *  fields=total,limit
         *  A dot separator can be used to specify non-reoccurring fields, while parentheses can be used to specify
         *  reoccurring fields within objects. For example, to get just the added date and user ID of the adder:
         *  fields=items(added_at,added_by.id)
         *  Use multiple parentheses to drill down into nested objects, for example:
         *  fields=items(track(name,href,album(name,href)))
         *  Fields can be excluded by prefixing them with an exclamation mark, for example:
         *  fields=items.track.album(!external_urls,images)
         * @param limit Optional. The maximum number of tracks to return. Default: 100. Minimum: 1. Maximum: 100.
         * @param offset Optional. The index of the first track to return. Default: 0 (the first object).
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking. For episodes, if a valid user access token is specified in the request
         *  header, the country associated with the user account will take priority over this parameter.
         *  _Note: If neither market or user country are provided, the episode is considered unavailable for the client.
         * @param additionalTypes Optional. A comma-separated list of item types that your client supports besides the
         *  default track type. Valid types are: track and episode. Note: This parameter was introduced to allow
         *  existing clients to maintain their current behaviour and might be deprecated in the future. In addition to
         *  providing this parameter, make sure that your client properly handles cases of new types in the future by
         *  checking against the type field of each object.
         */
        suspend fun getPlaylistTracks(
            playlistId: String,
            fields: List<String>? = null,
            limit: Int? = null,
            offset: Int? = null,
            market: String? = null,
            additionalTypes: List<String>? = null
        ): Paging<PlaylistTrack> {
            return get(
                "playlists/$playlistId/tracks",
                listOf(
                    "fields" to fields?.joinToString(separator = ","),
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString(),
                    "market" to market,
                    "additional_types" to additionalTypes?.joinToString(separator = ",")
                )
            )
        }
    }

    /**
     * Get Spotify Catalog information about albums, artists, playlists, tracks, shows or episodes that match a keyword
     * string.
     *
     * https://developer.spotify.com/documentation/web-api/reference/search/search/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-search
     */
    object Search {
        data class SearchResults(
            val albums: Paging<SimplifiedAlbum>?,
            val artists: Paging<FullArtist>?,
            val tracks: Paging<FullTrack>?,
            val playlists: Paging<SimplifiedPlaylist>?,
            val shows: Paging<SimplifiedShow>?,
            val episodes: Paging<SimplifiedEpisode>?
        )

        /**
         * Get Spotify Catalog information about albums, artists, playlists, tracks, shows or episodes that match a
         * keyword string.
         *
         * https://developer.spotify.com/documentation/web-api/reference/search/search/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-search
         *
         * @param q Required. Search query keywords and optional field filters and operators. For example:
         *  q=roadhouse%20blues.
         * @param type Required. A comma-separated list of item types to search across. Valid types are: album, artist,
         *  playlist, track, show and episode. Search results include hits from all the specified item types. For
         *  example: q=name:abacab&type=album,track returns both albums and tracks with “abacab” included in their name.
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. If a country code is
         *  specified, only artists, albums, and tracks with content that is playable in that market is returned. Note:
         *  - Playlist results are not affected by the market parameter.
         *  - If market is set to from_token, and a valid access token is specified in the request header, only content
         *    playable in the country associated with the user account, is returned.
         *  - Users can view the country that is associated with their account in the account settings. A user must
         *    grant access to the user-read-private scope prior to when the access token is issued.
         * @param limit Optional. Maximum number of results to return. Default: 20 Minimum: 1 Maximum: 50 Note: The
         *  limit is applied within each type, not on the total response. For example, if the limit value is 3 and the
         *  type is artist,album, the response contains 3 artists and 3 albums.
         * @param offset Optional. The index of the first result to return. Default: 0 (the first result). Maximum
         *  offset (including limit): 2,000. Use with limit to get the next page of search results.
         * @param includeExternal Optional. Possible values: audio If include_external=audio is specified the response
         *  will include any relevant audio content that is hosted externally. By default external content is filtered
         *  out from responses.
         */
        suspend fun search(
            q: String,
            type: List<String>,
            market: String? = null,
            limit: Int? = null,
            offset: Int? = null,
            includeExternal: String? = null
        ): SearchResults {
            return get(
                "search",
                listOf(
                    "q" to q,
                    "type" to type.joinToString(separator = ","),
                    "market" to market,
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString(),
                    "include_external" to includeExternal
                )
            )
        }
    }

    /**
     * Endpoints for retrieving information about one or more shows from the Spotify catalog.
     *
     * https://developer.spotify.com/documentation/web-api/reference/shows/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-shows
     */
    object Shows {
        /**
         * Get Spotify catalog information for a single show identified by its unique Spotify ID.
         *
         * https://developer.spotify.com/documentation/web-api/reference/shows/get-a-show/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-a-show
         *
         * @param id The Spotify ID for the show.
         * @param market Optional. An ISO 3166-1 alpha-2 country code. If a country code is specified, only shows and
         *  episodes that are available in that market will be returned. If a valid user access token is specified in
         *  the request header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the content is considered unavailable for the client.
         *  Users can view the country that is associated with their account in the account settings.
         */
        suspend fun getShow(id: String, market: String? = null): FullShow {
            return get("shows/$id", listOf("market" to market))
        }

        /**
         * Get Spotify catalog information for multiple shows based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/shows/get-several-shows/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-multiple-shows
         *
         * @param ids Required. A comma-separated list of the Spotify IDs for the shows. Maximum: 50 IDs.
         * @param market Optional. An ISO 3166-1 alpha-2 country code. If a country code is specified, only shows and
         *  episodes that are available in that market will be returned. If a valid user access token is specified in
         *  the request header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the content is considered unavailable for the client.
         *  Users can view the country that is associated with their account in the account settings.
         */
        suspend fun getShows(ids: List<String>, market: String? = null): List<SimplifiedShow> {
            return get<ShowsModel>(
                "shows",
                listOf("ids" to ids.joinToString(separator = ","), "market" to market)
            ).shows
        }

        /**
         * Get Spotify catalog information about a show’s episodes. Optional parameters can be used to limit the number
         * of episodes returned.
         *
         * https://developer.spotify.com/documentation/web-api/reference/shows/get-shows-episodes/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-a-shows-episodes
         *
         * @param id The Spotify ID for the show.
         * @param limit Optional. The maximum number of episodes to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first episode to return. Default: 0 (the first object). Use with
         *  limit to get the next set of episodes.
         * @param market Optional. An ISO 3166-1 alpha-2 country code. If a country code is specified, only shows and
         *  episodes that are available in that market will be returned. If a valid user access token is specified in
         *  the request header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the content is considered unavailable for the client.
         *  Users can view the country that is associated with their account in the account settings.
         */
        suspend fun getShowEpisodes(
            id: String,
            limit: Int? = null,
            offset: Int? = null,
            market: String? = null
        ): Paging<SimplifiedEpisode> {
            return get(
                "shows/$id/episodes",
                listOf("limit" to limit?.toString(), "offset" to offset?.toString(), "market" to market)
            )
        }
    }

    /**
     * Endpoints for retrieving information about one or more tracks from the Spotify catalog.
     *
     * https://developer.spotify.com/documentation/web-api/reference/tracks/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-tracks
     */
    object Tracks {
        /**
         * Get a detailed audio analysis for a single track identified by its unique Spotify ID.
         *
         * The Audio Analysis endpoint provides low-level audio analysis for all of the tracks in the Spotify catalog.
         * The Audio Analysis describes the track’s structure and musical content, including rhythm, pitch, and timbre.
         * All information is precise to the audio sample.
         *
         * Many elements of analysis include confidence values, a floating-point number ranging from 0.0 to 1.0.
         * Confidence indicates the reliability of its corresponding attribute. Elements carrying a small confidence
         * value should be considered speculative. There may not be sufficient data in the audio to compute the
         * attribute with high certainty.
         *
         * https://developer.spotify.com/documentation/web-api/reference/tracks/get-audio-analysis/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-audio-analysis
         *
         * @param id Required. The Spotify ID for the track.
         */
        suspend fun getAudioAnalysis(id: String): AudioAnalysis = get("audio-analysis/$id")

        /**
         * Get audio feature information for a single track identified by its unique Spotify ID.
         *
         * https://developer.spotify.com/documentation/web-api/reference/tracks/get-audio-features/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-audio-features
         *
         * @param id Required. The Spotify ID for the track.
         */
        suspend fun getAudioFeatures(id: String): AudioFeatures = get("audio-features/$id")

        /**
         * Get audio features for multiple tracks based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/tracks/get-several-audio-features/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-several-audio-features
         *
         * @param ids Required. A comma-separated list of the Spotify IDs for the tracks. Maximum: 100 IDs.
         */
        suspend fun getAudioFeatures(ids: List<String>): List<AudioFeatures> {
            return get<AudioFeaturesModel>(
                "audio-features",
                listOf("ids" to ids.joinToString(separator = ","))
            ).audioFeatures
        }

        /**
         * Get Spotify catalog information for a single track identified by its unique Spotify ID.
         *
         * https://developer.spotify.com/documentation/web-api/reference/tracks/get-track/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-track
         *
         * @param id The Spotify ID for the track.
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking.
         */
        suspend fun getTrack(id: String, market: String? = null): FullTrack {
            return get("tracks/$id", listOf("market" to market))
        }

        /**
         * Get Spotify catalog information for multiple tracks based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/tracks/get-several-tracks/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-several-tracks
         *
         * @param ids Required. A comma-separated list of the Spotify IDs for the tracks. Maximum: 50 IDs.
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking.
         */
        suspend fun getTracks(ids: List<String>, market: String? = null): List<FullTrack> {
            return get<TracksModel>(
                "tracks",
                listOf("ids" to ids.joinToString(separator = ","), "market" to market)
            ).tracks
        }
    }

    /**
     * Endpoints for retrieving information about a user’s profile.
     *
     * https://developer.spotify.com/documentation/web-api/reference/users-profile/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-users-profile
     */
    object UsersProfile {
        /**
         * Get detailed profile information about the current user (including the current user’s username).
         *
         * https://developer.spotify.com/documentation/web-api/reference/users-profile/get-current-users-profile/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-current-users-profile
         */
        suspend fun getCurrentUser(): PrivateUser {
            return get("me")
        }

        /**
         * Get public profile information about a Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/users-profile/get-users-profile/
         * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-users-profile
         *
         * @param userId The user’s Spotify user ID.
         */
        suspend fun getUser(userId: String): PublicUser {
            return get("users/$userId")
        }
    }
}
