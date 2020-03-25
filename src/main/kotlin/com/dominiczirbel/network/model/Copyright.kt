package com.dominiczirbel.network.model

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#copyright-object
 */
data class Copyright(
    /** The copyright text for this album. */
    val text: String,

    /** The type of copyright: C = the copyright, P = the sound recording (performance) copyright. */
    val type: String
)
