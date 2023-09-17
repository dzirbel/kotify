package com.dzirbel.kotify.network

import com.dzirbel.kotify.Runtime
import com.dzirbel.kotify.network.model.CursorPaging
import com.dzirbel.kotify.network.model.FullSpotifyAlbum
import com.dzirbel.kotify.network.model.FullSpotifyArtist
import com.dzirbel.kotify.network.model.FullSpotifyEpisode
import com.dzirbel.kotify.network.model.FullSpotifyPlaylist
import com.dzirbel.kotify.network.model.FullSpotifyShow
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.Paging
import com.dzirbel.kotify.network.model.PrivateSpotifyUser
import com.dzirbel.kotify.network.model.PublicSpotifyUser
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SimplifiedSpotifyEpisode
import com.dzirbel.kotify.network.model.SimplifiedSpotifyPlaylist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyShow
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyAudioAnalysis
import com.dzirbel.kotify.network.model.SpotifyAudioFeatures
import com.dzirbel.kotify.network.model.SpotifyCategory
import com.dzirbel.kotify.network.model.SpotifyImage
import com.dzirbel.kotify.network.model.SpotifyPlayHistoryObject
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlaybackOffset
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import com.dzirbel.kotify.network.model.SpotifyQueue
import com.dzirbel.kotify.network.model.SpotifyRecommendations
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.network.model.SpotifySavedAlbum
import com.dzirbel.kotify.network.model.SpotifySavedShow
import com.dzirbel.kotify.network.model.SpotifySavedTrack
import com.dzirbel.kotify.network.model.SpotifyTrackPlayback
import com.dzirbel.kotify.network.oauth.AccessToken
import com.dzirbel.kotify.network.util.await
import com.dzirbel.kotify.network.util.bodyFromJson
import com.dzirbel.kotify.network.util.json
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.Base64
import java.util.Locale
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

/**
 * https://developer.spotify.com/documentation/web-api/reference/
 *
 * TODO allow more lenient JSON parsing in release builds
 */
object Spotify {
    data class Configuration(
        val okHttpClient: OkHttpClient = OkHttpClient(),
        val oauthOkHttpClient: OkHttpClient = OkHttpClient(),
    )

    var configuration: Configuration = Configuration()

    /**
     * Whether executing network requests is allowed; defaults to false to prevent access from tests which should not
     * make accidental (and potentially expensive) calls to the network.
     */
    var enabled: Boolean = false

    const val FROM_TOKEN = "from_token"
    const val API_URL = "https://api.spotify.com/v1/"

    /**
     * The maximum number of sent/returned items for most endpoints.
     */
    const val MAX_LIMIT = 50

    val json by lazy { Runtime.json }

    class SpotifyError(val code: Int, message: String) : Throwable(message = "HTTP $code : $message") {
        @Serializable
        private data class ErrorObject(val error: Details) {
            @Serializable
            data class Details(val status: Int, val message: String)
        }

        companion object {
            fun from(response: Response): SpotifyError {
                val message = runCatching { response.bodyFromJson<ErrorObject>(json) }
                    .getOrNull()
                    ?.error
                    ?.message
                    ?: response.message
                return SpotifyError(code = response.code, message = message)
            }
        }
    }

    suspend inline fun <reified T : Any?> get(path: String, queryParams: Map<String, String?>? = null): T {
        return request(method = "GET", path = path, queryParams = queryParams, body = null)
    }

    suspend inline fun <reified In : Any?, reified Out> post(
        path: String,
        jsonBody: In,
        queryParams: Map<String, String?>? = null,
    ): Out {
        return request(
            method = "POST",
            path = path,
            queryParams = queryParams,
            body = json.encodeToString(jsonBody).toRequestBody(),
        )
    }

    suspend inline fun <reified In : Any?, reified Out> put(
        path: String,
        jsonBody: In,
        queryParams: Map<String, String?>? = null,
    ): Out {
        return request(
            method = "PUT",
            path = path,
            queryParams = queryParams,
            body = json.encodeToString(jsonBody).toRequestBody(),
        )
    }

    suspend inline fun <reified In : Any?, reified Out> delete(
        path: String,
        jsonBody: In,
        queryParams: Map<String, String?>? = null,
    ): Out {
        return request(
            method = "DELETE",
            path = path,
            queryParams = queryParams,
            body = json.encodeToString(jsonBody).toRequestBody(),
        )
    }

    suspend inline fun <reified T : Any?> request(
        method: String,
        path: String,
        queryParams: Map<String, String?>? = null,
        body: RequestBody? = null,
    ): T {
        // check that the request is not being done on the main dispatcher
        require(coroutineContext[ContinuationInterceptor] !is MainCoroutineDispatcher)

        check(enabled) { "Spotify network calls are not enabled" }

        val token = AccessToken.Cache.getOrThrow()

        val url = (if (path.startsWith(API_URL)) path else API_URL + path).toHttpUrl()
            .newBuilder()
            .apply {
                queryParams?.forEach { (key, value) ->
                    value?.let { addQueryParameter(key, it) }
                }
            }
            .build()

        val request = Request.Builder()
            .method(method, body)
            .url(url)
            .header("Authorization", "${token.tokenType} ${token.accessToken}")
            .build()

        return configuration.okHttpClient.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                throw SpotifyError.from(response)
            }

            response.bodyFromJson(json)
        }
    }

    /**
     * Endpoints for retrieving information about one or more albums from the Spotify catalog.
     */
    object Albums {
        /**
         * Get Spotify catalog information for a single album.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-an-album
         *
         * @param id The Spotify ID of the album.
         * @param market An ISO 3166-1 alpha-2 country code. If a country code is specified, only content that is
         *  available in that market will be returned. If a valid user access token is specified in the request header,
         *  the country associated with the user account will take priority over this parameter. Note: If neither market
         *  or user country are provided, the content is considered unavailable for the client. Users can view the
         *  country that is associated with their account in the account settings.
         */
        suspend fun getAlbum(id: String, market: String? = null): FullSpotifyAlbum {
            return get("albums/$id", mapOf("market" to market))
        }

        /**
         * Get Spotify catalog information for multiple albums identified by their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-multiple-albums
         *
         * @param ids A comma-separated list of the Spotify IDs for the albums. Maximum: 20 IDs.
         * @param market An ISO 3166-1 alpha-2 country code. If a country code is specified, only content that is
         *  available in that market will be returned. If a valid user access token is specified in the request header,
         *  the country associated with the user account will take priority over this parameter. Note: If neither market
         *  or user country are provided, the content is considered unavailable for the client. Users can view the
         *  country that is associated with their account in the account settings.
         */
        suspend fun getAlbums(ids: List<String>, market: String? = null): List<FullSpotifyAlbum> {
            @Serializable
            data class AlbumsModel(val albums: List<FullSpotifyAlbum>)

            return get<AlbumsModel>(
                "albums",
                mapOf("ids" to ids.joinToString(separator = ","), "market" to market),
            ).albums
        }

        /**
         * Get Spotify catalog information about an album’s tracks. Optional parameters can be used to limit the number
         * of tracks returned.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-an-albums-tracks
         *
         * @param id The Spotify ID of the album.
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
            market: String? = null,
        ): Paging<SimplifiedSpotifyTrack> {
            return get(
                "albums/$id/tracks",
                mapOf("limit" to limit?.toString(), "offset" to offset?.toString(), "market" to market),
            )
        }
    }

    /**
     * Endpoints for retrieving information about one or more artists from the Spotify catalog.
     */
    object Artists {
        /**
         * Get Spotify catalog information for a single artist identified by their unique Spotify ID.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-an-artist
         *
         * @param id The Spotify ID of the artist.
         */
        suspend fun getArtist(id: String): FullSpotifyArtist = get("artists/$id")

        /**
         * Get Spotify catalog information for several artists based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-multiple-artists
         *
         * @param ids A comma-separated list of the Spotify IDs for the artists. Maximum: 50 IDs.
         */
        suspend fun getArtists(ids: List<String>): List<FullSpotifyArtist> {
            @Serializable
            data class ArtistsModel(val artists: List<FullSpotifyArtist>)

            return get<ArtistsModel>("artists", mapOf("ids" to ids.joinToString(separator = ","))).artists
        }

        /**
         * Get Spotify catalog information about an artist's albums.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-an-artists-albums
         *
         * @param id The Spotify ID of the artist.
         * @param includeGroups A comma-separated list of keywords that will be used to filter the response. If not
         *  supplied, all album types will be returned.
         * @param market An ISO 3166-1 alpha-2 country code. If a country code is specified, only content that is
         *  available in that market will be returned. If a valid user access token is specified in the request header,
         *  the country associated with the user account will take priority over this parameter. Note: If neither market
         *  or user country are provided, the content is considered unavailable for the client. Users can view the
         *  country that is associated with their account in the account settings.
         * @param limit The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
         * @Param offset The index of the first item to return. Default: 0 (the first item). Use with limit to get the
         *  next set of items.
         */
        suspend fun getArtistAlbums(
            id: String,
            includeGroups: List<SpotifyAlbum.Type>? = null,
            market: String? = null,
            limit: Int? = null,
            offset: Int? = null,
        ): Paging<SimplifiedSpotifyAlbum> {
            return get(
                "artists/$id/albums",
                mapOf(
                    "include_groups" to includeGroups?.joinToString(separator = ",") { it.name.lowercase(Locale.US) },
                    "market" to market,
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString(),
                ),
            )
        }

        /**
         * Get Spotify catalog information about an artist's top tracks by country.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-an-artists-top-tracks
         *
         * @param id The Spotify ID of the artist.
         * @param market An ISO 3166-1 alpha-2 country code. If a country code is specified, only content that is
         *  available in that market will be returned. If a valid user access token is specified in the request header,
         *  the country associated with the user account will take priority over this parameter. Note: If neither market
         *  or user country are provided, the content is considered unavailable for the client. Users can view the
         *  country that is associated with their account in the account settings.
         */
        suspend fun getArtistTopTracks(id: String, market: String): List<FullSpotifyTrack> {
            @Serializable
            data class TracksModel(val tracks: List<FullSpotifyTrack>)

            return get<TracksModel>("artists/$id/top-tracks", mapOf("market" to market)).tracks
        }

        /**
         * Get Spotify catalog information about artists similar to a given artist. Similarity is based on analysis of
         * the Spotify community's listening history.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-an-artists-related-artists
         *
         * @param id The Spotify ID of the artist.
         */
        suspend fun getArtistRelatedArtists(id: String): List<FullSpotifyArtist> {
            @Serializable
            data class ArtistsModel(val artists: List<FullSpotifyArtist>)

            return get<ArtistsModel>("artists/$id/related-artists").artists
        }
    }

    /**
     * Endpoints for getting playlists and new album releases featured on Spotify’s Browse tab.
     */
    object Browse {
        /**
         * Get a single category used to tag items in Spotify (on, for example, the Spotify player’s “Browse” tab).
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-a-category
         *
         * @param categoryId The Spotify category ID for the category.
         * @param country A country: an ISO 3166-1 alpha-2 country code. Provide this parameter to ensure that the
         *  category exists for a particular country.
         * @param locale The desired language, consisting of an ISO 639-1 language code and an ISO 3166-1 alpha-2
         *  country code, joined by an underscore. For example: es_MX, meaning "Spanish (Mexico)". Provide this
         *  parameter if you want the category strings returned in a particular language. Note: if locale is not
         *  supplied, or if the specified language is not available, the category strings returned will be in the
         *  Spotify default language (American English).
         */
        suspend fun getCategory(categoryId: String, country: String? = null, locale: String? = null): SpotifyCategory {
            return get("browse/categories/$categoryId", mapOf("country" to country, "locale" to locale))
        }

        /**
         * Get a list of categories used to tag items in Spotify (on, for example, the Spotify player’s “Browse” tab).
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-categories
         *
         * @param country A country: an ISO 3166-1 alpha-2 country code. Provide this parameter if you want to narrow
         *  the list of returned categories to those relevant to a particular country. If omitted, the returned items
         *  will be globally relevant.
         * @param locale The desired language, consisting of an ISO 639-1 language code and an ISO 3166-1 alpha-2
         *  country code, joined by an underscore. For example: es_MX, meaning "Spanish (Mexico)". Provide this
         *  parameter if you want the category metadata returned in a particular language. Note: if locale is not
         *  supplied, or if the specified language is not available, all strings will be returned in the Spotify default
         *  language (American English). The locale parameter, combined with the country parameter, may give odd results
         *  if not carefully matched. For example country=SE&locale=de_DE will return a list of categories relevant to
         *  Sweden but as German language strings.
         * @param limit The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset The index of the first item to return. Default: 0 (the first item). Use with limit to get the
         *  next set of items.
         */
        suspend fun getCategories(
            country: String? = null,
            locale: String? = null,
            limit: Int? = null,
            offset: Int? = null,
        ): Paging<SpotifyCategory> {
            @Serializable
            data class CategoriesModel(val categories: Paging<SpotifyCategory>)

            return get<CategoriesModel>(
                "browse/categories",
                mapOf(
                    "country" to country,
                    "locale" to locale,
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString(),
                ),
            ).categories
        }

        /**
         * Get a list of Spotify playlists tagged with a particular category.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-a-categories-playlists
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
            offset: Int? = null,
        ): Paging<SimplifiedSpotifyPlaylist?> {
            @Serializable
            data class PlaylistPagingModel(
                val playlists: Paging<SimplifiedSpotifyPlaylist?>,
                val message: String? = null,
            )

            return get<PlaylistPagingModel>(
                "browse/categories/$categoryId/playlists",
                mapOf("country" to country, "limit" to limit?.toString(), "offset" to offset?.toString()),
            ).playlists
        }

        /**
         * Get a list of Spotify featured playlists (shown, for example, on a Spotify player's 'Browse' tab).
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-featured-playlists
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
            offset: Int? = null,
        ): Paging<SimplifiedSpotifyPlaylist?> {
            @Serializable
            data class PlaylistPagingModel(
                val playlists: Paging<SimplifiedSpotifyPlaylist?>,
                val message: String? = null,
            )

            return get<PlaylistPagingModel>(
                "browse/featured-playlists",
                mapOf(
                    "locale" to locale,
                    "country" to country,
                    "timestamp" to timestamp,
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString(),
                ),
            ).playlists
        }

        /**
         * Get a list of new album releases featured in Spotify (shown, for example, on a Spotify player’s “Browse”
         * tab).
         *
         *https://developer.spotify.com/documentation/web-api/reference/get-new-releases
         *
         * @param country A country: an ISO 3166-1 alpha-2 country code. Provide this parameter if you want the list of
         *  returned items to be relevant to a particular country. If omitted, the returned items will be relevant to
         *  all countries.
         * @param limit The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset The index of the first item to return. Default: 0 (the first item). Use with limit to get the
         *  next set of items.
         */
        suspend fun getNewReleases(
            country: String? = null,
            limit: Int? = null,
            offset: Int? = null,
        ): Paging<SimplifiedSpotifyAlbum> {
            @Serializable
            data class AlbumsPagingModel(val albums: Paging<SimplifiedSpotifyAlbum>)

            return get<AlbumsPagingModel>(
                "browse/new-releases",
                mapOf("country" to country, "limit" to limit?.toString(), "offset" to offset?.toString()),
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
         * https://developer.spotify.com/documentation/web-api/reference/get-recommendations
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
            tunableTrackAttributes: Map<String, String> = emptyMap(),
        ): SpotifyRecommendations {
            return get(
                "recommendations",
                mapOf(
                    "limit" to limit?.toString(),
                    "market" to market,
                    "seed_artists" to seedArtists.joinToString(separator = ","),
                    "seed_genres" to seedGenres.joinToString(separator = ","),
                    "seed_tracks" to seedTracks.joinToString(separator = ","),
                ).plus(tunableTrackAttributes),
            )
        }

        /**
         * Retrieve a list of available genres seed parameter values for recommendations.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-recommendation-genres
         */
        suspend fun getRecommendationGenres(): List<String> {
            @Serializable
            data class RecommendationGenresModel(val genres: List<String>)

            return get<RecommendationGenresModel>("recommendations/available-genre-seeds").genres
        }

        /**
         * Get the list of markets where Spotify is available.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-available-markets
         */
        suspend fun getAvailableMarkets(): List<String> {
            @Serializable
            data class AvailableMarketsModel(val markets: List<String>)

            return get<AvailableMarketsModel>("markets").markets
        }
    }

    /**
     * Endpoints for retrieving information about one or more episodes from the Spotify catalog.
     */
    object Episodes {
        /**
         * Get Spotify catalog information for a single episode identified by its unique Spotify ID.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-an-episode
         *
         * @param id The Spotify ID for the episode.
         * @param market Optional. An ISO 3166-1 alpha-2 country code. If a country code is specified, only shows and
         *  episodes that are available in that market will be returned. If a valid user access token is specified in
         *  the request header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the content is considered unavailable for the client.
         *  Users can view the country that is associated with their account in the account settings.
         */
        suspend fun getEpisode(id: String, market: String? = null): FullSpotifyEpisode {
            return get("episodes/$id", mapOf("market" to market))
        }

        /**
         * Get Spotify catalog information for multiple episodes based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-multiple-episodes
         *
         * @param ids Required. A comma-separated list of the Spotify IDs for the episodes. Maximum: 50 IDs.
         * @param market Optional. An ISO 3166-1 alpha-2 country code. If a country code is specified, only shows and
         *  episodes that are available in that market will be returned. If a valid user access token is specified in
         *  the request header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the content is considered unavailable for the client.
         *  Users can view the country that is associated with their account in the account settings.
         */
        suspend fun getEpisodes(ids: List<String>, market: String? = null): List<FullSpotifyEpisode?> {
            @Serializable
            data class EpisodesModel(val episodes: List<FullSpotifyEpisode?>)

            return get<EpisodesModel>(
                "episodes",
                mapOf("ids" to ids.joinToString(separator = ","), "market" to market),
            ).episodes
        }
    }

    /**
     * Endpoints for managing the artists, users, and playlists that a Spotify user follows.
     */
    object Follow {
        @Serializable
        data class ArtistsCursorPagingModel(val artists: CursorPaging<FullSpotifyArtist>)

        /**
         * Check to see if the current user is following one or more artists or other Spotify users.
         *
         * https://developer.spotify.com/documentation/web-api/reference/check-current-user-follows
         *
         * @param type Required. The ID type: either artist or user.
         * @param ids Required. A comma-separated list of the artist or the user Spotify IDs to check. For example:
         *  ids=74ASZWbe4lXaubB36ztrGX,08td7MxkoHQkXnWAYD8d6Q. A maximum of 50 IDs can be sent in one request.
         */
        suspend fun isFollowing(type: String, ids: List<String>): List<Boolean> {
            return get("me/following/contains", mapOf("type" to type, "ids" to ids.joinToString(separator = ",")))
        }

        /**
         * Check to see if one or more Spotify users are following a specified playlist.
         *
         * https://developer.spotify.com/documentation/web-api/reference/check-if-user-follows-playlist
         *
         * @param playlistId The Spotify ID of the playlist.
         * @param userIds Required. A comma-separated list of Spotify User IDs ; the ids of the users that you want to
         *  check to see if they follow the playlist. Maximum: 5 ids.
         */
        suspend fun isFollowingPlaylist(playlistId: String, userIds: List<String>): List<Boolean> {
            return get(
                "playlists/$playlistId/followers/contains",
                mapOf("ids" to userIds.joinToString(separator = ",")),
            )
        }

        /**
         * Get the current user’s followed artists.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-followed
         *
         * @param limit Optional. The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param after Optional. The last artist ID retrieved from the previous request.
         */
        suspend fun getFollowedArtists(limit: Int? = null, after: String? = null): CursorPaging<FullSpotifyArtist> {
            return get<ArtistsCursorPagingModel>(
                "me/following",
                mapOf("type" to "artist", "limit" to limit?.toString(), "after" to after),
            ).artists
        }

        /**
         * Add the current user as a follower of one or more artists or other Spotify users.
         *
         * https://developer.spotify.com/documentation/web-api/reference/follow-artists-users
         *
         * @param type Required. The ID type: either artist or user.
         * @param ids Optional. A comma-separated list of the artist or the user Spotify IDs. For example:
         *  ids=74ASZWbe4lXaubB36ztrGX,08td7MxkoHQkXnWAYD8d6Q. A maximum of 50 IDs can be sent in one request.
         */
        suspend fun follow(type: String, ids: List<String>) {
            put<Unit?, Unit>(
                "me/following",
                jsonBody = null,
                queryParams = mapOf("type" to type, "ids" to ids.joinToString(separator = ",")),
            )
        }

        /**
         * Add the current user as a follower of a playlist.
         *
         * https://developer.spotify.com/documentation/web-api/reference/follow-playlist
         *
         * @param playlistId The Spotify ID of the playlist. Any playlist can be followed, regardless of its
         *  public/private status, as long as you know its playlist ID.
         * @param public Optional. Defaults to true. If true the playlist will be included in user’s public playlists,
         *  if false it will remain private. To be able to follow playlists privately, the user must have granted the
         *  playlist-modify-private scope.
         */
        suspend fun followPlaylist(playlistId: String, public: Boolean = true) {
            put<_, Unit>("playlists/$playlistId/followers", jsonBody = mapOf("public" to public))
        }

        /**
         * Remove the current user as a follower of one or more artists or other Spotify users.
         *
         * https://developer.spotify.com/documentation/web-api/reference/unfollow-artists-users
         *
         * @param type Required. The ID type: either artist or user.
         * @param ids Optional. A comma-separated list of the artist or the user Spotify IDs. For example:
         *  ids=74ASZWbe4lXaubB36ztrGX,08td7MxkoHQkXnWAYD8d6Q. A maximum of 50 IDs can be sent in one request.
         */
        suspend fun unfollow(type: String, ids: List<String>) {
            delete<Unit?, Unit>(
                "me/following",
                jsonBody = null,
                queryParams = mapOf("type" to type, "ids" to ids.joinToString(separator = ",")),
            )
        }

        /**
         * Remove the current user as a follower of a playlist.
         *
         * https://developer.spotify.com/documentation/web-api/reference/unfollow-playlist
         *
         * @param playlistId The Spotify ID of the playlist that is to be no longer followed.
         */
        suspend fun unfollowPlaylist(playlistId: String) {
            delete<Unit?, Unit>(
                "playlists/$playlistId/followers",
                jsonBody = null,
                queryParams = null,
            )
        }
    }

    /**
     * Endpoints for retrieving information about, and managing, tracks that the current user has saved in their "Your
     * Music" library.
     */
    object Library {
        const val CHECK_TRACKS_PATH = "me/tracks/contains"
        const val CHECK_ALBUMS_PATH = "me/albums/contains"

        /**
         * Get a list of the albums saved in the current Spotify user's 'Your Music' library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-users-saved-albums
         *
         * @param limit The maximum number of objects to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset The index of the first object to return. Default: 0 (i.e., the first object). Use with limit to
         *  get the next set of objects.
         * @param market An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter if you want
         *  to apply Track Relinking.
         */
        suspend fun getSavedAlbums(
            limit: Int? = null,
            offset: Int? = null,
            market: String? = null,
        ): Paging<SpotifySavedAlbum> {
            return get(
                "me/albums",
                mapOf("limit" to limit?.toString(), "offset" to offset?.toString(), "market" to market),
            )
        }

        /**
         * Save one or more albums to the current user's 'Your Music' library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/save-albums-user
         *
         * @param ids A comma-separated list of the Spotify IDs. Maximum: 50 IDs.
         */
        suspend fun saveAlbums(ids: List<String>) {
            put<_, Unit>("me/albums", jsonBody = mapOf("ids" to ids))
        }

        /**
         * Remove one or more albums from the current user's 'Your Music' library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/remove-albums-user
         *
         * @param ids A comma-separated list of the Spotify IDs. Maximum: 50 IDs.
         */
        suspend fun removeAlbums(ids: List<String>) {
            delete<_, Unit>("me/albums", jsonBody = mapOf("ids" to ids))
        }

        /**
         * Check if one or more albums is already saved in the current Spotify user’s ‘Your Music’ library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/check-users-saved-albums
         *
         * @param ids A comma-separated list of the Spotify IDs for the albums. Maximum: 20 IDs.
         */
        suspend fun checkAlbums(ids: List<String>): List<Boolean> {
            return get(CHECK_ALBUMS_PATH, mapOf("ids" to ids.joinToString(separator = ",")))
        }

        /**
         * Get a list of the songs saved in the current Spotify user’s ‘Your Music’ library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/check-users-saved-tracks
         *
         * @param limit The maximum number of objects to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset The index of the first object to return. Default: 0 (i.e., the first object). Use with limit to
         *  get the next set of objects.
         * @param market An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter if you want
         *  to apply Track Relinking.
         */
        suspend fun getSavedTracks(
            limit: Int? = null,
            offset: Int? = null,
            market: String? = null,
        ): Paging<SpotifySavedTrack> {
            return get(
                "me/tracks",
                mapOf("limit" to limit?.toString(), "offset" to offset?.toString(), "market" to market),
            )
        }

        /**
         * Save one or more tracks to the current user’s ‘Your Music’ library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/save-tracks-user
         *
         * @param ids A comma-separated list of the Spotify IDs. Maximum: 50 IDs.
         */
        suspend fun saveTracks(ids: List<String>) {
            put<_, Unit>("me/tracks", jsonBody = mapOf("ids" to ids))
        }

        /**
         * Remove one or more tracks from the current user’s ‘Your Music’ library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/remove-tracks-user
         *
         * @param ids A comma-separated list of the Spotify IDs. Maximum: 50 IDs.
         */
        suspend fun removeTracks(ids: List<String>) {
            return delete("me/tracks", jsonBody = mapOf("ids" to ids))
        }

        /**
         * Check if one or more tracks is already saved in the current Spotify user’s ‘Your Music’ library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/check-users-saved-tracks
         *
         * @param ids A comma-separated list of the Spotify IDs. Maximum: 50 IDs.
         */
        suspend fun checkTracks(ids: List<String>): List<Boolean> {
            return get(CHECK_TRACKS_PATH, mapOf("ids" to ids.joinToString(separator = ",")))
        }

        /**
         * Get a list of shows saved in the current Spotify user’s library. Optional parameters can be used to limit the
         * number of shows returned.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-users-saved-shows
         *
         * @param limit The maximum number of shows to return. Default: 20. Minimum: 1. Maximum: 50
         * @param offset The index of the first show to return. Default: 0 (the first object). Use with limit to get the
         *  next set of shows.
         */
        suspend fun getSavedShows(limit: Int? = null, offset: Int? = null): Paging<SpotifySavedShow> {
            return get("me/shows", mapOf("limit" to limit?.toString(), "offset" to offset?.toString()))
        }

        /**
         * Save one or more shows to current Spotify user’s library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/save-shows-user
         *
         * @param ids A comma-separated list of the Spotify IDs. Maximum: 50 IDs.
         */
        suspend fun saveShows(ids: List<String>) {
            put<Unit?, Unit>(
                "me/shows",
                queryParams = mapOf("ids" to ids.joinToString(separator = ",")),
                jsonBody = null,
            )
        }

        /**
         * Delete one or more shows from current Spotify user’s library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/remove-shows-user
         *
         * @param ids A comma-separated list of the Spotify IDs. Maximum: 50 IDs.
         */
        suspend fun removeShows(ids: List<String>) {
            delete<Unit?, Unit>(
                "me/shows",
                queryParams = mapOf("ids" to ids.joinToString(separator = ",")),
                jsonBody = null,
            )
        }

        /**
         * Check if one or more shows is already saved in the current Spotify user’s library.
         *
         * https://developer.spotify.com/documentation/web-api/reference/check-users-saved-shows
         *
         * @param ids A comma-separated list of the Spotify IDs. Maximum: 50 IDs.
         */
        suspend fun checkShows(ids: List<String>): List<Boolean> {
            return get("me/shows/contains", mapOf("ids" to ids.joinToString(separator = ",")))
        }
    }

    object Player {
        const val GET_CURRENT_PLAYBACK_PATH = "me/player"
        const val GET_CURRENT_PLAYING_TRACK_PATH = "me/player/currently-playing"
        const val GET_AVAILABLE_DEVICES_PATH = "me/player/devices"
        const val START_PLAYBACK_PATH = "me/player/play"
        const val PAUSE_PLAYBACK_PATH = "me/player/pause"
        const val TRANSFER_PLAYBACK_PATH = "me/player"
        const val SKIP_TO_NEXT_PATH = "me/player/next"
        const val SKIP_TO_PREVIOUS_PATH = "me/player/previous"
        const val TOGGLE_SHUFFLE_PATH = "me/player/shuffle"
        const val SET_REPEAT_MODE_PATH = "me/player/repeat"
        const val SET_VOLUME_PATH = "me/player/volume"
        const val SEEK_TO_POSITION_PATH = "me/player/seek"

        @Serializable
        data class AvailableDevicesResponse(val devices: List<SpotifyPlaybackDevice>)

        /**
         * Get information about the user’s current playback state, including track or episode, progress, and active
         * device.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-information-about-the-users-current-
         * playback
         *
         * @param market An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter if you want
         *  to apply Track Relinking.
         * @param additionalTypes A comma-separated list of item types that your client supports besides the default
         *  track type. Valid types are: track and episode. An unsupported type in the response is expected to be
         *  represented as null value in the item field. Note: This parameter was introduced to allow existing clients
         *  to maintain their current behaviour and might be deprecated in the future. In addition to providing this
         *  parameter, make sure that your client properly handles cases of new
         */
        suspend fun getCurrentPlayback(
            market: String? = null,
            additionalTypes: List<String>? = null,
        ): SpotifyPlayback? {
            return get(
                GET_CURRENT_PLAYBACK_PATH,
                mapOf("market" to market, "additional_types" to additionalTypes?.joinToString(separator = ",")),
            )
        }

        /**
         * Transfer playback to a new device and determine if it should start playing.
         *
         * https://developer.spotify.com/documentation/web-api/reference/transfer-a-users-playback
         *
         * @param deviceIds A JSON array containing the ID of the device on which playback should be
         *  started/transferred. For example:{device_ids:["74ASZWbe4lXaubB36ztrGX"]}
         *  Note: Although an array is accepted, only a single device_id is currently supported. Supplying more than one
         *  will return 400 Bad Request
         * @param play true: ensure playback happens on new device. false or not provided: keep the current playback
         *  state.
         */
        suspend fun transferPlayback(deviceIds: List<String>, play: Boolean? = null) {
            @Serializable
            data class Body(
                @SerialName("device_ids") val deviceIds: List<String>,
                val play: Boolean? = null,
            )

            put<_, Unit>(TRANSFER_PLAYBACK_PATH, jsonBody = Body(deviceIds = deviceIds, play = play))
        }

        /**
         * Get information about a user’s available devices.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-a-users-available-devices
         */
        suspend fun getAvailableDevices(): List<SpotifyPlaybackDevice> {
            return get<AvailableDevicesResponse>(GET_AVAILABLE_DEVICES_PATH).devices
        }

        /**
         * Get the object currently being played on the user’s Spotify account.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-the-users-currently-playing-track
         *
         * @param market An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter if you want
         *  to apply Track Relinking.
         * @param additionalTypes A comma-separated list of item types that your client supports besides the default
         *  track type. Valid types are: track and episode. An unsupported type in the response is expected to be
         *  represented as null value in the item field. Note: This parameter was introduced to allow existing clients
         *  to maintain their current behaviour and might be deprecated in the future. In addition to providing this
         *  parameter, make sure that your client properly handles cases of new types in the future by checking against
         *  the currently_playing_type field.
         */
        suspend fun getCurrentlyPlayingTrack(
            market: String? = null,
            additionalTypes: List<String>? = null,
        ): SpotifyTrackPlayback? {
            return get(
                GET_CURRENT_PLAYING_TRACK_PATH,
                mapOf("market" to market, "additional_types" to additionalTypes?.joinToString(separator = ",")),
            )
        }

        /**
         * Start a new context or resume current playback on the user’s active device.
         *
         * https://developer.spotify.com/documentation/web-api/reference/start-a-users-playback
         *
         * @param deviceId The id of the device this command is targeting. If not supplied, the user’s currently active
         *  device is the target.
         * @param contextUri string
         * @param uris Array of URIs
         * @param offset object
         * @param positionMs integer
         */
        suspend fun startPlayback(
            deviceId: String? = null,
            contextUri: String? = null,
            uris: List<String>? = null,
            offset: SpotifyPlaybackOffset? = null,
            positionMs: Int? = null,
        ) {
            @Serializable
            data class Body(
                @SerialName("context_uri") val contextUri: String? = null,
                @SerialName("uris") val uris: List<String>? = null,
                @SerialName("offset") val offset: SpotifyPlaybackOffset? = null,
                @SerialName("position_ms") val positionMs: Int? = null,
            )

            put<_, Unit>(
                START_PLAYBACK_PATH,
                jsonBody = Body(contextUri = contextUri, uris = uris, offset = offset, positionMs = positionMs),
                queryParams = mapOf("device_id" to deviceId),
            )
        }

        /**
         * Pause playback on the user’s account.
         *
         * https://developer.spotify.com/documentation/web-api/reference/pause-a-users-playback
         *
         * @param deviceId The id of the device this command is targeting. If not supplied, the user’s currently active
         *  device is the target.
         */
        suspend fun pausePlayback(deviceId: String? = null) {
            put<Unit?, Unit>(
                PAUSE_PLAYBACK_PATH,
                jsonBody = null,
                queryParams = mapOf("device_id" to deviceId),
            )
        }

        /**
         * Skips to next track in the user’s queue.
         *
         * https://developer.spotify.com/documentation/web-api/reference/skip-users-playback-to-next-track
         *
         * @param deviceId The id of the device this command is targeting. If not supplied, the user’s currently active
         *  device is the target.
         */
        suspend fun skipToNext(deviceId: String? = null) {
            post<Unit?, Unit>(
                SKIP_TO_NEXT_PATH,
                jsonBody = null,
                queryParams = mapOf("device_id" to deviceId),
            )
        }

        /**
         * Skips to previous track in the user’s queue.
         *
         * https://developer.spotify.com/documentation/web-api/reference/skip-users-playback-to-previous-track
         *
         * @param deviceId The id of the device this command is targeting. If not supplied, the user’s currently active
         *  device is the target.
         */
        suspend fun skipToPrevious(deviceId: String? = null) {
            post<Unit?, Unit>(
                SKIP_TO_PREVIOUS_PATH,
                jsonBody = null,
                queryParams = mapOf("device_id" to deviceId),
            )
        }

        /**
         * Seeks to the given position in the user’s currently playing track.
         *
         * https://developer.spotify.com/documentation/web-api/reference/seek-to-position-in-currently-playing-track
         *
         * @param positionMs The position in milliseconds to seek to. Must be a positive number. Passing in a position
         *  that is greater than the length of the track will cause the player to start playing the next song.
         * @param deviceId The id of the device this command is targeting. If not supplied, the user’s currently active
         *  device is the target.
         */
        suspend fun seekToPosition(positionMs: Int, deviceId: String? = null) {
            put<Unit?, Unit>(
                SEEK_TO_POSITION_PATH,
                jsonBody = null as Unit?,
                queryParams = mapOf("position_ms" to positionMs.toString(), "device_id" to deviceId),
            )
        }

        /**
         * Set the repeat mode for the user’s playback. Options are repeat-track, repeat-context, and off.
         *
         * https://developer.spotify.com/documentation/web-api/reference/set-repeat-mode-on-users-playback
         *
         * @param state track, context or off.
         *  track will repeat the current track.
         *  context will repeat the current context.
         *  off will turn repeat off.
         * @param deviceId The id of the device this command is targeting. If not supplied, the user’s currently active
         *  device is the target.
         */
        suspend fun setRepeatMode(state: SpotifyRepeatMode, deviceId: String? = null) {
            put<Unit?, Unit>(
                SET_REPEAT_MODE_PATH,
                jsonBody = null,
                queryParams = mapOf("state" to state.name.lowercase(Locale.US), "device_id" to deviceId),
            )
        }

        /**
         * Set the volume for the user’s current playback device.
         *
         * https://developer.spotify.com/documentation/web-api/reference/set-volume-for-users-playback
         *
         * @param volumePercent The volume to set. Must be a value from 0 to 100 inclusive.
         * @param deviceId The id of the device this command is targeting. If not supplied, the user’s currently active
         *  device is the target.
         */
        suspend fun setVolume(volumePercent: Int, deviceId: String? = null) {
            put<Unit?, Unit>(
                SET_VOLUME_PATH,
                jsonBody = null,
                queryParams = mapOf("volume_percent" to volumePercent.toString(), "device_id" to deviceId),
            )
        }

        /**
         * Toggle shuffle on or off for user’s playback.
         *
         * https://developer.spotify.com/documentation/web-api/reference/toggle-shuffle-for-users-playback
         *
         * @param state true : Shuffle user’s playback. false : Do not shuffle user’s playback.
         * @param deviceId The id of the device this command is targeting. If not supplied, the user’s currently active
         *  device is the target.
         */
        suspend fun toggleShuffle(state: Boolean, deviceId: String? = null) {
            put<Unit?, Unit>(
                TOGGLE_SHUFFLE_PATH,
                jsonBody = null,
                queryParams = mapOf("state" to state.toString(), "device_id" to deviceId),
            )
        }

        /**
         * Get tracks from the current user’s recently played tracks. Note: Currently doesn't support podcast episodes.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-recently-played
         *
         * @param limit The maximum number of items to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param after A Unix timestamp in milliseconds. Returns all items after (but not including) this cursor
         *  position. If after is specified, before must not be specified.
         * @param before A Unix timestamp in milliseconds. Returns all items before (but not including) this cursor
         *  position. If before is specified, after must not be specified.
         */
        suspend fun getRecentlyPlayedTracks(
            limit: Int? = null,
            after: Long? = null,
            before: Long? = null,
        ): CursorPaging<SpotifyPlayHistoryObject> {
            return get(
                "me/player/recently-played",
                queryParams = mapOf(
                    "limit" to limit?.toString(),
                    "after" to after?.toString(),
                    "before" to before?.toString(),
                ),
            )
        }

        /**
         * Get the list of objects that make up the user's queue.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-queue
         */
        suspend fun getQueue(): SpotifyQueue {
            return get("me/player/queue")
        }

        /**
         * Add an item to the end of the user’s current playback queue.
         *
         * https://developer.spotify.com/documentation/web-api/reference/add-to-queue
         *
         * @param uri The uri of the item to add to the queue. Must be a track or an episode uri.
         * @param deviceId The id of the device this command is targeting. If not supplied, the user’s currently active
         *  device is the target.
         */
        suspend fun addItemToQueue(uri: String, deviceId: String? = null) {
            post<Unit?, Unit>(
                "me/player/queue",
                jsonBody = null,
                queryParams = mapOf("uri" to uri, "device_id" to deviceId),
            )
        }
    }

    /**
     * Endpoints for retrieving information about a user’s playlists and for managing a user’s playlists.
     */
    object Playlists {
        @Serializable
        private data class SnaphshotId(@SerialName("snapshot_id") val snapshotId: String)

        /**
         * Get a list of the playlists owned or followed by the current Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-a-list-of-current-users-playlists
         *
         * @param limit Optional. The maximum number of playlists to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first playlist to return. Default: 0 (the first object). Maximum
         *  offset: 100.000. Use with limit to get the next set of playlists.
         */
        suspend fun getPlaylists(limit: Int? = null, offset: Int? = null): Paging<SimplifiedSpotifyPlaylist> {
            return get("me/playlists", mapOf("limit" to limit?.toString(), "offset" to offset?.toString()))
        }

        /**
         * Get a list of the playlists owned or followed by a Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-list-users-playlists
         *
         * @param userId The user's Spotify user ID.
         * @param limit Optional. The maximum number of playlists to return. Default: 20. Minimum: 1. Maximum: 50.
         * @param offset Optional. The index of the first playlist to return. Default: 0 (the first object). Maximum
         *  offset: 100.000. Use with limit to get the next set of playlists.
         */
        suspend fun getPlaylists(
            userId: String,
            limit: Int? = null,
            offset: Int? = null,
        ): Paging<SimplifiedSpotifyPlaylist> {
            return get("users/$userId/playlists", mapOf("limit" to limit?.toString(), "offset" to offset?.toString()))
        }

        /**
         * Create a playlist for a Spotify user. (The playlist will be empty until you add tracks.)
         *
         * https://developer.spotify.com/documentation/web-api/reference/create-playlist
         *
         * @param userId The user's Spotify user ID.
         * @param name The name for the new playlist, for example "Your Coolest Playlist" . This name does not need to
         *  be unique; a user may have several playlists with the same name.
         * @param public Defaults to true. If true the playlist will be public, if false it will be private. To be able
         *  to create private playlists, the user must have granted the playlist-modify-private scope.
         * @param collaborative Defaults to false. If true the playlist will be collaborative. Note that to create a
         *  collaborative playlist you must also set public to false . To create collaborative playlists you must have
         *  granted playlist-modify-private and playlist-modify-public scopes.
         * @param description value for playlist description as displayed in Spotify Clients and in the Web API.
         */
        suspend fun createPlaylist(
            userId: String,
            name: String,
            public: Boolean? = null,
            collaborative: Boolean? = null,
            description: String? = null,
        ): FullSpotifyPlaylist {
            return post(
                "users/$userId/playlists",
                jsonBody = mapOf(
                    "name" to name,
                    "public" to public?.toString(),
                    "collaborative" to collaborative?.toString(),
                    "description" to description,
                ),
            )
        }

        /**
         * Change a playlist’s name and public/private state. (The user must, of course, own the playlist.)
         *
         * https://developer.spotify.com/documentation/web-api/reference/change-playlist-details
         *
         * @param playlistId The Spotify ID for the playlist.
         * @param name The new name for the playlist, for example "My New Playlist Title"
         * @param public If true the playlist will be public, if false it will be private.
         * @param collaborative If true, the playlist will become collaborative and other users will be able to modify
         *  the playlist in their Spotify client. Note: You can only set collaborative to true on non-public playlists.
         * @param description Value for playlist description as displayed in Spotify Clients and in the Web API.
         */
        suspend fun changePlaylistDetails(
            playlistId: String,
            name: String? = null,
            public: Boolean? = null,
            collaborative: Boolean? = null,
            description: String? = null,
        ) {
            @Serializable
            data class Body(
                val name: String? = null,
                val public: Boolean? = null,
                val collaborative: Boolean? = null,
                val description: String? = null,
            )

            put<_, Unit>(
                "playlists/$playlistId",
                jsonBody = Body(
                    name = name,
                    public = public,
                    collaborative = collaborative,
                    description = description,
                ),
            )
        }

        /**
         * Add one or more items to a user’s playlist.
         *
         * https://developer.spotify.com/documentation/web-api/reference/add-tracks-to-playlist
         *
         * @param playlistId The Spotify ID for the playlist.
         * @param position The position to insert the items, a zero-based index. For example, to insert the items in the
         *  first position: position=0; to insert the items in the third position: position=2 . If omitted, the items
         *  will be appended to the playlist. Items are added in the order they are listed in the query string or
         *  request body.
         * @param uris A JSON array of the Spotify URIs to add. A maximum of 100 items can be added in one request.
         */
        suspend fun addItemsToPlaylist(playlistId: String, position: Int? = null, uris: List<String>): String {
            @Serializable
            data class Body(val position: Int? = null, val uris: List<String>)

            return post<_, SnaphshotId>(
                "playlists/$playlistId/tracks",
                jsonBody = Body(position = position, uris = uris),
            ).snapshotId
        }

        /**
         * https://developer.spotify.com/documentation/web-api/reference/reorder-or-replace-playlists-tracks
         *
         * @param playlistId The Spotify ID for the playlist.
         * @param rangeStart The position of the first item to be reordered.
         * @param insertBefore The position where the items should be inserted. To reorder the items to the end of the
         *  playlist, simply set insert_before to the position after the last item. Examples: To reorder the first item
         *  to the last position in a playlist with 10 items, set range_start to 0, and insert_before to 10. To reorder
         *  the last item in a playlist with 10 items to the start of the playlist, set range_start to 9, and
         *  insert_before to 0.
         * @param rangeLength The amount of items to be reordered. Defaults to 1 if not set. The range of items to be
         *  reordered begins from the range_start position, and includes the range_length subsequent items. Example: To
         *  move the items at index 9-10 to the start of the playlist, range_start is set to 9, and range_length is set
         *  to 2.
         * @param snapshotId The playlist’s snapshot ID against which you want to make the changes.
         */
        suspend fun reorderPlaylistItems(
            playlistId: String,
            rangeStart: Int? = null,
            insertBefore: Int? = null,
            rangeLength: Int? = null,
            snapshotId: String? = null,
        ): String {
            @Serializable
            data class Body(
                @SerialName("range_start") val rangeStart: Int? = null,
                @SerialName("insert_before") val insertBefore: Int? = null,
                @SerialName("range_length") val rangeLength: Int? = null,
                @SerialName("snapshot_id") val snapshotId: String? = null,
            )

            return put<_, SnaphshotId>(
                "playlists/$playlistId/tracks",
                jsonBody = Body(
                    rangeStart = rangeStart,
                    insertBefore = insertBefore,
                    rangeLength = rangeLength,
                    snapshotId = snapshotId,
                ),
            ).snapshotId
        }

        /**
         * https://developer.spotify.com/documentation/web-api/reference/reorder-or-replace-playlists-tracks
         *
         * @param playlistId The Spotify ID for the playlist.
         * @param uris A comma-separated list of Spotify URIs to set, can be track or episode URIs. A maximum of 100
         *  items can be set in one request.
         */
        suspend fun replacePlaylistItems(playlistId: String, uris: List<String>): String {
            @Serializable
            data class Body(val uris: List<String>)

            return put<_, SnaphshotId>(
                "playlists/$playlistId/tracks",
                jsonBody = Body(uris = uris),
            ).snapshotId
        }

        /**
         * https://developer.spotify.com/documentation/web-api/reference/remove-tracks-playlist
         *
         * @param playlistId The Spotify ID
         * @param tracks An array of objects containing Spotify URIs of the tracks or episodes to remove. A maximum of
         *  100 objects can be sent at once.
         * @param snapshotId The playlist’s snapshot ID against which you want to make the changes. The API will
         *  validate that the specified items exist and in the specified positions and make the changes, even if more
         *  recent changes have been made to the playlist.
         */
        suspend fun removePlaylistTracks(playlistId: String, tracks: List<String>, snapshotId: String? = null): String {
            @Serializable
            data class Body(val uris: List<String>, val snapshotId: String? = null)

            return delete<_, SnaphshotId>(
                "playlists/$playlistId/tracks",
                jsonBody = Body(uris = tracks, snapshotId = snapshotId),
            ).snapshotId
        }

        /**
         * Get the current image associated with a specific playlist.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-playlist-cover
         *
         * @param playlistId The Spotify ID for the playlist.
         */
        suspend fun getPlaylistCoverImages(playlistId: String): List<SpotifyImage> {
            return get("playlists/$playlistId/images")
        }

        /**
         * Replace the image used to represent a specific playlist.
         *
         * https://developer.spotify.com/documentation/web-api/reference/upload-custom-playlist-cover
         *
         * @param playlistId The Spotify ID for the playlist.
         * @param jpegImage The request should contain a Base64 encoded JPEG image data, maximum payload size is 256 KB.
         */
        suspend fun uploadPlaylistCoverImage(playlistId: String, jpegImage: ByteArray) {
            return request(
                method = "PUT",
                path = "playlists/$playlistId/images",
                body = Base64.getEncoder().encodeToString(jpegImage)
                    .toRequestBody(contentType = "image/jpeg".toMediaType()),
            )
        }

        /**
         * Get a playlist owned by a Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-playlist
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
            additionalTypes: List<String>? = null,
        ): FullSpotifyPlaylist {
            return get(
                "playlists/$playlistId",
                mapOf(
                    "fields" to fields?.joinToString(separator = ","),
                    "market" to market,
                    "additional_types" to additionalTypes?.joinToString(separator = ","),
                ),
            )
        }

        /**
         * Get full details of the items of a playlist owned by a Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-playlists-tracks
         *
         * @param playlistId The Spotify ID of the playlist.
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
            additionalTypes: List<String>? = null,
        ): Paging<SpotifyPlaylistTrack> {
            return get(
                "playlists/$playlistId/tracks",
                mapOf(
                    "fields" to fields?.joinToString(separator = ","),
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString(),
                    "market" to market,
                    "additional_types" to additionalTypes?.joinToString(separator = ","),
                ),
            )
        }
    }

    /**
     * Get Spotify Catalog information about albums, artists, playlists, tracks, shows or episodes that match a keyword
     * string.
     */
    object Search {
        @Serializable
        data class SearchResults(
            val albums: Paging<SimplifiedSpotifyAlbum>? = null,
            val artists: Paging<FullSpotifyArtist>? = null,
            val tracks: Paging<FullSpotifyTrack>? = null,
            val playlists: Paging<SimplifiedSpotifyPlaylist>? = null,
            val shows: Paging<SimplifiedSpotifyShow>? = null,
            val episodes: Paging<SimplifiedSpotifyEpisode>? = null,
        )

        /**
         * Get Spotify Catalog information about albums, artists, playlists, tracks, shows or episodes that match a
         * keyword string.
         *
         * https://developer.spotify.com/documentation/web-api/reference/search
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
            includeExternal: String? = null,
        ): SearchResults {
            return get(
                "search",
                mapOf(
                    "q" to q,
                    "type" to type.joinToString(separator = ","),
                    "market" to market,
                    "limit" to limit?.toString(),
                    "offset" to offset?.toString(),
                    "include_external" to includeExternal,
                ),
            )
        }
    }

    /**
     * Endpoints for retrieving information about one or more shows from the Spotify catalog.
     */
    object Shows {
        /**
         * Get Spotify catalog information for a single show identified by its unique Spotify ID.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-a-show
         *
         * @param id The Spotify ID for the show.
         * @param market Optional. An ISO 3166-1 alpha-2 country code. If a country code is specified, only shows and
         *  episodes that are available in that market will be returned. If a valid user access token is specified in
         *  the request header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the content is considered unavailable for the client.
         *  Users can view the country that is associated with their account in the account settings.
         */
        suspend fun getShow(id: String, market: String? = null): FullSpotifyShow {
            return get("shows/$id", mapOf("market" to market))
        }

        /**
         * Get Spotify catalog information for multiple shows based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-multiple-shows
         *
         * @param ids Required. A comma-separated list of the Spotify IDs for the shows. Maximum: 50 IDs.
         * @param market Optional. An ISO 3166-1 alpha-2 country code. If a country code is specified, only shows and
         *  episodes that are available in that market will be returned. If a valid user access token is specified in
         *  the request header, the country associated with the user account will take priority over this parameter.
         *  Note: If neither market or user country are provided, the content is considered unavailable for the client.
         *  Users can view the country that is associated with their account in the account settings.
         */
        suspend fun getShows(ids: List<String>, market: String? = null): List<SimplifiedSpotifyShow> {
            @Serializable
            data class ShowsModel(val shows: List<SimplifiedSpotifyShow>)

            return get<ShowsModel>(
                "shows",
                mapOf("ids" to ids.joinToString(separator = ","), "market" to market),
            ).shows
        }

        /**
         * Get Spotify catalog information about a show’s episodes. Optional parameters can be used to limit the number
         * of episodes returned.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-a-shows-episodes
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
            market: String? = null,
        ): Paging<SimplifiedSpotifyEpisode> {
            return get(
                "shows/$id/episodes",
                mapOf("limit" to limit?.toString(), "offset" to offset?.toString(), "market" to market),
            )
        }
    }

    /**
     * Endpoints for retrieving information about one or more tracks from the Spotify catalog.
     */
    object Tracks {
        /**
         * Get Spotify catalog information for a single track identified by its unique Spotify ID.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-track
         *
         * @param id The Spotify ID for the track.
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking.
         */
        suspend fun getTrack(id: String, market: String? = null): FullSpotifyTrack {
            return get("tracks/$id", mapOf("market" to market))
        }

        /**
         * Get Spotify catalog information for multiple tracks based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-several-tracks
         *
         * @param ids Required. A comma-separated list of the Spotify IDs for the tracks. Maximum: 50 IDs.
         * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter
         *  if you want to apply Track Relinking.
         */
        suspend fun getTracks(ids: List<String>, market: String? = null): List<FullSpotifyTrack> {
            @Serializable
            data class TracksModel(val tracks: List<FullSpotifyTrack>)

            return get<TracksModel>(
                "tracks",
                mapOf("ids" to ids.joinToString(separator = ","), "market" to market),
            ).tracks
        }

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
         * https://developer.spotify.com/documentation/web-api/reference/get-audio-analysis
         *
         * @param id Required. The Spotify ID for the track.
         */
        suspend fun getAudioAnalysis(id: String): SpotifyAudioAnalysis = get("audio-analysis/$id")

        /**
         * Get audio feature information for a single track identified by its unique Spotify ID.
         *
         *https://developer.spotify.com/documentation/web-api/reference/get-audio-features
         *
         * @param id Required. The Spotify ID for the track.
         */
        suspend fun getAudioFeatures(id: String): SpotifyAudioFeatures = get("audio-features/$id")

        /**
         * Get audio features for multiple tracks based on their Spotify IDs.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-several-audio-features
         *
         * @param ids Required. A comma-separated list of the Spotify IDs for the tracks. Maximum: 100 IDs.
         */
        suspend fun getAudioFeatures(ids: List<String>): List<SpotifyAudioFeatures> {
            @Serializable
            data class AudioFeaturesModel(@SerialName("audio_features") val audioFeatures: List<SpotifyAudioFeatures>)

            return get<AudioFeaturesModel>(
                "audio-features",
                mapOf("ids" to ids.joinToString(separator = ",")),
            ).audioFeatures
        }
    }

    /**
     * Endpoints for retrieving information about a user’s profile.
     */
    object UsersProfile {
        enum class TimeRange(val value: String) {
            SHORT_TERM("short_term"),
            MEDIUM_TERM("medium_term"),
            LONG_TERM("long_term"),
        }

        /**
         * Get detailed profile information about the current user (including the current user’s username).
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-current-users-profile
         */
        suspend fun getCurrentUser(): PrivateSpotifyUser {
            return get("me")
        }

        /**
         * Get public profile information about a Spotify user.
         *
         * https://developer.spotify.com/documentation/web-api/reference/get-users-profile
         *
         * @param userId The user’s Spotify user ID.
         */
        suspend fun getUser(userId: String): PublicSpotifyUser {
            return get("users/$userId")
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
         * https://developer.spotify.com/documentation/web-api/reference/get-users-top-artists-and-tracks
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
            timeRange: TimeRange? = null,
        ): Paging<FullSpotifyArtist> {
            return get(
                "me/top/artists",
                mapOf("limit" to limit?.toString(), "offset" to offset?.toString(), "time_range" to timeRange?.value),
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
         * https://developer.spotify.com/documentation/web-api/reference/get-users-top-artists-and-tracks
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
            timeRange: TimeRange? = null,
        ): Paging<FullSpotifyTrack> {
            return get(
                "me/top/tracks",
                mapOf("limit" to limit?.toString(), "offset" to offset?.toString(), "time_range" to timeRange?.value),
            )
        }
    }
}
