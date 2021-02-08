package com.dominiczirbel.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResumePoint(
    /** Whether or not the episode has been fully played by the user. */
    @SerialName("fully_played") val fullyPlayed: Boolean,

    /** The user’s most recent position in the episode in milliseconds. */
    @SerialName("resume_position_ms") val resumePositionMs: Int
)
