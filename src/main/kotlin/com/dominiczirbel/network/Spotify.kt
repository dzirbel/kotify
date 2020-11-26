package com.dominiczirbel.network

import com.dominiczirbel.network.model.Album
import com.dominiczirbel.network.model.AudioAnalysis
import com.dominiczirbel.network.model.AudioFeatures
import com.dominiczirbel.network.model.Category
import com.dominiczirbel.network.model.FullAlbum
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.network.model.FullPlaylist
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.Image
import com.dominiczirbel.network.model.Paging
import com.dominiczirbel.network.model.PlaylistTrack
import com.dominiczirbel.network.model.Recommendations
import com.dominiczirbel.network.model.SimplifiedAlbum
import com.dominiczirbel.network.model.SimplifiedPlaylist
import com.dominiczirbel.network.model.SimplifiedTrack
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.await
import com.github.kittinunf.fuel.gson.gsonDeserializer
import com.github.kittinunf.fuel.httpGet
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import java.util.Locale

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/
 * https://developer.spotify.com/documentation/web-api/reference-beta
 */
object Spotify {
    internal val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    private val errorDeserializer = gsonDeserializer<ErrorObject>(gson)

    const val FROM_TOKEN = "from_token"
    private const val API_URL = "https://api.spotify.com/v1/"

    class SpotifyError(val code: Int, message: String, cause: Throwable) :
        Throwable(message = "HTTP $code : $message", cause = cause)

    private data class ErrorObject(val error: ErrorDetails)
    private data class ErrorDetails(val status: Int, val message: String)

    private data class AlbumsModel(val albums: List<FullAlbum>)
    private data class AlbumsPagingModel(val albums: Paging<SimplifiedAlbum>)
    private data class ArtistsModel(val artists: List<FullArtist>)
    private data class AudioFeaturesModel(val audioFeatures: List<AudioFeatures>)
    private data class CategoriesModel(val categories: Paging<Category>)
    private data class PlaylistPagingModel(val playlists: Paging<SimplifiedPlaylist>)
    private data class TracksModel(val tracks: List<FullTrack>)

    private suspend inline fun <reified T : Any> get(path: String, queryParams: List<Pair<String, Any?>>? = null): T {
        val token = AccessToken.getCachedOrThrow()

        return try {
            (API_URL + path).httpGet(queryParams)
                .header("Authorization", "${token.tokenType} ${token.accessToken}")
                .await(gsonDeserializer(gson))
        } catch (ex: FuelError) {
            val message = if (ex.response.body().isConsumed()) {
                ex.message ?: ex.response.body().toString()
            } else {
                errorDeserializer.deserialize(ex.response).error.message
            }
            throw SpotifyError(code = ex.response.statusCode, message = message, cause = ex)
        }
    }

    /**
     * Retrieve an [AccessToken] based on the given [clientId] and [clientSecret]. This [AccessToken] will automatically
     * be applied to future requests.
     *
     * https://developer.spotify.com/documentation/general/guides/authorization-guide/
     */
    suspend fun authenticate(clientId: String, clientSecret: String): AccessToken {
        return AccessToken.getOrThrow(clientId, clientSecret)
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
            return get("albums/$id/tracks", listOf("limit" to limit, "offset" to offset, "market" to market))
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
                    "limit" to limit,
                    "offset" to offset
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
                listOf("country" to country, "limit" to limit, "offset" to offset)
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
                listOf("country" to country, "locale" to locale, "limit" to limit, "offset" to offset)
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
                    "limit" to limit,
                    "offset" to offset
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
                listOf("country" to country, "limit" to limit, "offset" to offset)
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
                    "limit" to limit,
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
        // TODO add episodes endpoints
    }

    /**
     * Endpoints for managing the artists, users, and playlists that a Spotify user follows.
     *
     * https://developer.spotify.com/documentation/web-api/reference/follow/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-follow
     */
    object Follow {
        // TODO add follow endpoints
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
        // TODO add personalization endpoints
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
     * TODO add tests
     *
     * TODO https://developer.spotify.com/documentation/web-api/reference/playlists/add-tracks-to-playlist/
     * TODO https://developer.spotify.com/documentation/web-api/reference/playlists/change-playlist-details/
     * TODO https://developer.spotify.com/documentation/web-api/reference/playlists/create-playlist/
     * TODO https://developer.spotify.com/documentation/web-api/reference/playlists/remove-tracks-playlist/
     * TODO https://developer.spotify.com/documentation/web-api/reference/playlists/reorder-playlists-tracks/
     * TODO https://developer.spotify.com/documentation/web-api/reference/playlists/replace-playlists-tracks/
     * TODO https://developer.spotify.com/documentation/web-api/reference/playlists/upload-custom-playlist-cover/
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
            return get("me/playlists", listOf("limit" to limit, "offset" to offset))
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
            return get("users/$userId/playlists", listOf("limit" to limit, "offset" to offset))
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
                    "limit" to limit,
                    "offset" to offset,
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
        // TODO add search endpoints
    }

    /**
     * Endpoints for retrieving information about one or more shows from the Spotify catalog.
     *
     * https://developer.spotify.com/documentation/web-api/reference/shows/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#category-shows
     */
    object Shows {
        // TODO add shows endpoints
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
        // TODO add users profile endpoints
    }
}
