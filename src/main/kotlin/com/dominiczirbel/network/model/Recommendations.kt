package com.dominiczirbel.network.model

import kotlinx.serialization.Serializable

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-recommendationsobject
 */
@Serializable
data class Recommendations(
    /**	An array of recommendation seed objects. */
    val seeds: List<RecommendationSeed>,

    /** An array of track object (simplified) ordered according to the parameters supplied. */
    val tracks: List<SimplifiedTrack>
)
