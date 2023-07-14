package com.dzirbel.kotify.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class SpotifyPlaylistTrack(
    /**
     * The date and time the track or episode was added.
     * Note that some very old playlists may return null in this field.
     */
    @SerialName("added_at") val addedAt: String?,

    /**
     * The Spotify user who added the track or episode.
     * Note that some very old playlists may return null in this field.
     */
    @SerialName("added_by") val addedBy: PublicSpotifyUser?,

    /** Whether this track or episode is a local file or not. */
    @SerialName("is_local") val isLocal: Boolean,

    /** Information about the track or episode. */
    @SerialName("track") private val trackOrEpisode: JsonElement,

    /** Undocumented field. */
    @SerialName("primary_color") val primaryColor: String? = null,

    /** Undocumented field. */
    @SerialName("video_thumbnail") val videoThumbnail: Map<String, String?>? = null,
) {
    /**
     * The [SimplifiedSpotifyTrack] wrapped in this [SpotifyPlaylistTrack], may be null if it is an [episode] instead.
     */
    val track: SimplifiedSpotifyTrack? by lazy {
        @Suppress("SwallowedException")
        try {
            Json.decodeFromJsonElement<SimplifiedSpotifyTrack>(trackOrEpisode)
        } catch (ex: SerializationException) {
            null
        }
    }

    /**
     * The [SimplifiedSpotifyEpisode] wrapped in this [SpotifyPlaylistTrack], may be null if it is an [track] instead.
     */
    val episode: SimplifiedSpotifyEpisode? by lazy {
        @Suppress("SwallowedException")
        try {
            Json.decodeFromJsonElement<SimplifiedSpotifyEpisode>(trackOrEpisode)
        } catch (ex: SerializationException) {
            null
        }
    }
}
