package com.dzirbel.kotify.network.model

import kotlinx.serialization.Serializable

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-recommendationseedobject
 */
@Serializable
data class RecommendationSeed(
    /** The number of tracks available after min_* and max_* filters have been applied. */
    val afterFilteringSize: Int,

    /** The number of tracks available after relinking for regional availability. */
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
    val initialPoolSize: Int,

    /** The entity type of this seed. One of artist, track or genre. */
    val type: String
)
