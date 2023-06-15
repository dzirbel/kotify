package com.dzirbel.kotify.network.model

import kotlinx.serialization.Serializable

@Serializable
data class SpotifyQueue(
    /**
     * The currently playing track or episode. Can be null.
     */
    val currentlyPlaying: FullSpotifyTrack?,

    /**
     * The tracks or episodes in the queue. Can be empty.
     */
    val queue: List<FullSpotifyTrack>,
)
