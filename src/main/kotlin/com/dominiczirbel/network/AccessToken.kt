package com.dominiczirbel.network

import com.dominiczirbel.Secrets
import com.github.kittinunf.fuel.core.await
import com.github.kittinunf.fuel.gson.gsonDeserializer
import com.github.kittinunf.fuel.httpPost
import com.google.gson.annotations.SerializedName
import java.util.Base64
import java.util.concurrent.TimeUnit

data class AccessToken(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Long
) {
    private val received: Long = System.currentTimeMillis()

    val isExpired
        get() = System.currentTimeMillis() > received + TimeUnit.SECONDS.toMillis(expiresIn)

    companion object {
        private val base64Encoder = Base64.getEncoder()

        // TODO ensure we don't make two concurrent requests for the access token
        private var current: AccessToken? = null

        suspend fun get(): AccessToken? {
            current?.takeIf { !it.isExpired }?.let { return it }

            val unencodedAuth = Secrets["client-id"] + ":" + Secrets["client-secret"]
            val encodedAuth = base64Encoder.encodeToString(unencodedAuth.toByteArray())

            return "https://accounts.spotify.com/api/token".httpPost()
                .body("grant_type=client_credentials")
                .header("Authorization", "Basic $encodedAuth")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .await(gsonDeserializer<AccessToken>())
                .also { current = it }
        }

        suspend fun getOrThrow(): AccessToken = get() ?: throw NoAccessTokenError

        object NoAccessTokenError : Throwable()
    }
}
