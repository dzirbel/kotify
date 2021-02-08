package com.dominiczirbel.network.model

import kotlinx.serialization.Serializable

@Serializable
data class Recommendations(
    /**	An array of recommendation seed objects. */
    val seeds: List<RecommendationsSeed>,

    /** An array of track object (simplified) ordered according to the parameters supplied. */
    val tracks: List<SimplifiedTrack>
)
