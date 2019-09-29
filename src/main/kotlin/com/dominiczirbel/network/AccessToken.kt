package com.dominiczirbel.network

import com.dominiczirbel.Secrets
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.net.HttpURLConnection
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64
import java.util.concurrent.TimeUnit

data class AccessToken(
    val value: String,
    val type: String,
    val expiration: Long
) {
    val isExpired
        get() = System.currentTimeMillis() > expiration

    companion object {
        private val httpClient = HttpClient.newHttpClient()
        private val base64Encoder = Base64.getEncoder()
        private val gson = Gson()

        fun get(): AccessToken? {
            val unencodedAuth = Secrets["client-id"] + ":" + Secrets["client-secret"]
            val encodedAuth = base64Encoder.encodeToString(unencodedAuth.toByteArray())
            val request = HttpRequest.newBuilder()
                .uri(URI("https://accounts.spotify.com/api/token"))
                .header("Authorization", "Basic $encodedAuth")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build()

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                .takeIf { it.statusCode() == HttpURLConnection.HTTP_OK }
                ?.let { gson.fromJson(it.body(), Response::class.java) }
                ?.let { response ->
                    AccessToken(
                        value = response.accessToken,
                        type = response.tokenType,
                        // assumes that expires_in is in seconds, documentation doesn't specify (value is 3600)
                        expiration = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(response.expiresIn)
                    )
                }
        }
    }

    private data class Response(
        @SerializedName("access_token") val accessToken: String,
        @SerializedName("token_type") val tokenType: String,
        @SerializedName("expires_in") val expiresIn: Long
    )
}
