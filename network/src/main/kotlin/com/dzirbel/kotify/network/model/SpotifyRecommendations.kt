package com.dzirbel.kotify.network.model

import kotlinx.serialization.Serializable

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-recommendationsobject
 */
@Serializable
data class SpotifyRecommendations(
    /**	An array of recommendation seed objects. */
    val seeds: List<SpotifyRecommendationSeed>,

    /** An array of track object (simplified) ordered according to the parameters supplied. */
    val tracks: List<SimplifiedSpotifyTrack>,
)
