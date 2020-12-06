package com.dominiczirbel.network.model

import com.google.gson.annotations.SerializedName

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#recommendations-seed-object
 */
data class RecommendationsSeed(
    /** The number of tracks available after min_* and max_* filters have been applied. */
    @SerializedName("afterFilteringSize")
    val afterFilteringSize: Int,

    /** The number of tracks available after relinking for regional availability. */
    @SerializedName("afterRelinkingSize")
    val afterRelinkingSize: Int,

    /**
     * A link to the full track or artist data for this seed. For tracks this will be a link to a Track Object. For
     * artists a link to an Artist Object. For genre seeds, this value will be null.
     */
    val href: String?,

    /**
     * The id used to select this seed. This will be the same as the string used in the seed_artists, seed_tracks or
     * seed_genres parameter.
     */
    val id: String,

    /** The number of recommended tracks available for this seed. */
    @SerializedName("initialPoolSize")
    val initialPoolSize: Int,

    /** The entity type of this seed. One of artist, track or genre. */
    val type: String
)
