package com.dominiczirbel.network

import com.dominiczirbel.network.model.FullTrack
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.await
import com.github.kittinunf.fuel.gson.gsonDeserializer
import com.github.kittinunf.fuel.httpGet

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/
 * https://developer.spotify.com/documentation/web-api/reference-beta
 */
object Spotify {
    private val errorDeserializer = gsonDeserializer<ErrorObject>()

    class SpotifyError(code: Int, message: String, cause: Throwable) :
        Throwable(message = "HTTP $code : $message", cause = cause)

    private data class ErrorObject(val error: ErrorDetails)
    private data class ErrorDetails(val status: Int, val message: String)
    private data class Tracks(val tracks: List<FullTrack>)

    private suspend inline fun <reified T : Any> get(url: String, queryParams: List<Pair<String, String?>>? = null): T {
        val token = AccessToken.getOrThrow()

        return try {
            url.httpGet(queryParams)
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
        return get("https://api.spotify.com/v1/tracks/$id", listOf("market" to market))
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
            "https://api.spotify.com/v1/tracks",
            listOf("ids" to ids.joinToString(separator = ","), "market" to market)
        ).tracks
    }
}
