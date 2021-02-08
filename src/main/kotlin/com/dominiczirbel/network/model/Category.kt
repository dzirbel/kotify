package com.dominiczirbel.network.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    /** A link to the Web API endpoint returning full details of the category. */
    val href: String,

    /** The category icon, in various sizes. */
    val icons: List<Image>,

    /** The Spotify category ID of the category. */
    val id: String,

    /** The name of the category. */
    val name: String
)
