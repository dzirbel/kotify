package com.dominiczirbel.network

import com.dominiczirbel.network.model.Track
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object Spotify {
    private val httpClient = HttpClient.newHttpClient()
    private val gson = Gson()

    private var accessToken: AccessToken? = null
        get() {
            return field?.takeIf { !it.isExpired }
                ?: AccessToken.get().also { field = it }
        }

    fun getTrack(id: String): Track? {
        val token = accessToken?.value ?: return null

        val request = HttpRequest.newBuilder()
            .uri(URI("https://api.spotify.com/v1/tracks/$id"))
            .header("Authorization", "Bearer $token")
            .GET()
            .build()

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            .takeIf { it.statusCode() == HttpURLConnection.HTTP_OK }
            ?.let { gson.fromJson(it.body(), Track::class.java) }
    }
}
