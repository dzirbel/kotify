package com.dominiczirbel.network

import com.dominiczirbel.network.model.FullTrack
import com.github.kittinunf.fuel.core.await
import com.github.kittinunf.fuel.gson.gsonDeserializer
import com.github.kittinunf.fuel.httpGet

object Spotify {
    suspend fun getTrack(id: String): FullTrack? {
        val token = AccessToken.getOrThrow()

        return "https://api.spotify.com/v1/tracks/$id".httpGet()
            .header("Authorization", "Bearer ${token.accessToken}")
            .await(gsonDeserializer())
    }
}
