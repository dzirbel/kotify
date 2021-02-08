package com.dominiczirbel.network.model

import kotlinx.serialization.Serializable

@Serializable
data class TimeInterval(
    /** The starting point (in seconds) of the time interval. */
    val start: Float,

    /** The duration (in seconds) of the time interval. */
    val duration: Float,

    /** The confidence, from 0.0 to 1.0, of the reliability of the interval. */
    val confidence: Float
)
