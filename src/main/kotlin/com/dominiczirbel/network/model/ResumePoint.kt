package com.dominiczirbel.network.model

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#resume-point-object
 */
data class ResumePoint(
    /** Whether or not the episode has been fully played by the user. */
    val fullyPlayed: Boolean,

    /** The user’s most recent position in the episode in milliseconds. */
    val resumePositionMs: Int
)
