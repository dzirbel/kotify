package com.dominiczirbel.network

import com.dominiczirbel.network.model.FullTrack
import com.github.kittinunf.fuel.core.await
import com.github.kittinunf.fuel.gson.gsonDeserializer
import com.github.kittinunf.fuel.httpGet

object Spotify {
    private suspend inline fun <reified T : Any> get(url: String): T {
        val token = AccessToken.getOrThrow()
        return url.httpGet()
            .header("Authorization", "${token.tokenType} ${token.accessToken}")
            .await(gsonDeserializer())
    }

    suspend fun getTrack(id: String): FullTrack = get("https://api.spotify.com/v1/tracks/$id")
}
