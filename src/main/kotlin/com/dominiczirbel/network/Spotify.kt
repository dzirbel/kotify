package com.dominiczirbel.network

import com.dominiczirbel.network.model.Track
import com.github.kittinunf.fuel.core.await
import com.github.kittinunf.fuel.gson.gsonDeserializer
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson

object Spotify {
    private val gson = Gson()
    private val trackDeserializer = gsonDeserializer<Track>(gson)

    suspend fun getTrack(id: String): Track? {
        val token = AccessToken.getOrThrow()

        return "https://api.spotify.com/v1/tracks/$id".httpGet()
            .header("Authorization", "Bearer ${token.accessToken}")
            .await(trackDeserializer)
    }
}
