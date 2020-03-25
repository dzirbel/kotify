package com.dominiczirbel.network

import com.dominiczirbel.network.model.Album
import com.dominiczirbel.network.model.FullArtist
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.Paging
import com.dominiczirbel.network.model.SimplifiedAlbum
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.await
import com.github.kittinunf.fuel.gson.gsonDeserializer
import com.github.kittinunf.fuel.httpGet
import java.util.Locale

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/
 * https://developer.spotify.com/documentation/web-api/reference-beta
 */
object Spotify {
    private val errorDeserializer = gsonDeserializer<ErrorObject>()

    const val MARKET_FROM_TOKEN = "from_token"
    private const val API_URL = "https://api.spotify.com/v1/"

    class SpotifyError(val code: Int, message: String, cause: Throwable) :
        Throwable(message = "HTTP $code : $message", cause = cause)

    private data class ErrorObject(val error: ErrorDetails)
    private data class ErrorDetails(val status: Int, val message: String)

    private data class Artists(val artists: List<FullArtist>)
    private data class Tracks(val tracks: List<FullTrack>)

    private suspend inline fun <reified T : Any> get(url: String, queryParams: List<Pair<String, Any?>>? = null): T {
        val token = AccessToken.getOrThrow()

        return try {
            (API_URL + url).httpGet(queryParams)
                .header("Authorization", "${token.tokenType} ${token.accessToken}")
                .await(gsonDeserializer())
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
     * Get Spotify catalog information for a single artist identified by their unique Spotify ID.
     *
     * https://developer.spotify.com/documentation/web-api/reference/artists/get-artist/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-artist
     *
     * @param id The Spotify ID of the artist.
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
        return get<Artists>("artists", listOf("ids" to ids.joinToString(separator = ","))).artists
    }

    /**
     * Get Spotify catalog information about an artist’s albums. Optional parameters can be specified in the query
     * string to filter and sort the response.
     *
     * https://developer.spotify.com/documentation/web-api/reference/artists/get-artists-albums/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-artists-albums
     *
     * @param id The Spotify ID for the artist.
     * @param includeGroups A comma-separated list of keywords that will be used to filter the response. If not
     *  supplied, all album types will be returned. Valid values are:
     *  - album
     *  - single
     *  - appears_on
     *  - compilation
     * @param market Synonym for country. An ISO 3166-1 alpha-2 country code or the string from_token.
     *  Supply this parameter to limit the response to one particular geographical market. For example, for albums
     *  available in Sweden: market=SE.
     *  If not given, results will be returned for all markets and you are likely to get duplicate results per album,
     *  one for each market in which the album is available!
     * @param limit The number of album objects to return. Default: 20. Minimum: 1. Maximum: 50. For example: limit=2
     * @Param offset The index of the first album to return. Default: 0 (i.e., the first album). Use with limit to get
     *  the next set of albums.
     */
    suspend fun getArtistAlbums(
        id: String,
        includeGroups: List<Album.Type>? = null,
        market: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Paging<SimplifiedAlbum> {
        return get(
            "artists/$id/albums",
            listOf(
                "include_groups" to includeGroups?.joinToString(separator = ",") { it.name.toLowerCase(Locale.US) },
                "market" to market,
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
     * @param market An ISO 3166-1 alpha-2 country code or the string from_token. Synonym for country.
     */
    suspend fun getArtistTopTracks(id: String, market: String? = null): List<FullTrack> {
        return get<Tracks>("artists/$id/top-tracks", listOf("market" to market)).tracks
    }

    /**
     * Get Spotify catalog information about artists similar to a given artist. Similarity is based on analysis of the
     * Spotify community’s listening history.
     *
     * https://developer.spotify.com/documentation/web-api/reference/artists/get-related-artists/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-an-artists-top-tracks
     *
     * @param id The Spotify ID for the artist
     */
    suspend fun getArtistRelatedArtists(id: String): List<FullArtist> {
        return get<Artists>("artists/$id/related-artists").artists
    }

    /**
     * Get Spotify catalog information for a single track identified by its unique Spotify ID.
     *
     * https://developer.spotify.com/documentation/web-api/reference/tracks/get-track/
     * https://developer.spotify.com/documentation/web-api/reference-beta/#endpoint-get-track
     *
     * @param id The Spotify ID for the track.
     * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter if
     *  you want to apply Track Relinking.
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
     * @param market Optional. An ISO 3166-1 alpha-2 country code or the string from_token. Provide this parameter if
     *  you want to apply Track Relinking.
     */
    suspend fun getTracks(ids: List<String>, market: String? = null): List<FullTrack> {
        return get<Tracks>(
            "tracks",
            listOf("ids" to ids.joinToString(separator = ","), "market" to market)
        ).tracks
    }
}
