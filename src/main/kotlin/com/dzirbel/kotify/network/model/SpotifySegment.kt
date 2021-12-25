package com.dzirbel.kotify.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpotifySegment(
    /** The starting point (in seconds) of the segment. */
    val start: Float,

    /** The duration (in seconds) of the segment. */
    val duration: Float,

    /**
     * The confidence, from 0.0 to 1.0, of the reliability of the segmentation. Segments of the song which are difficult
     * to logically segment (e.g: noise) may correspond to low values in this field.
     */
    val confidence: Float,

    /**
     * The onset loudness of the segment in decibels (dB). Combined with loudness_max and loudness_max_time, these
     * components can be used to describe the “attack” of the segment.
     */
    @SerialName("loudness_start") val loudnessStart: Float,

    /**
     * The peak loudness of the segment in decibels (dB). Combined with loudness_start and loudness_max_time, these
     * components can be used to describe the “attack” of the segment.
     */
    @SerialName("loudness_max") val loudnessMax: Float,

    /**
     * The segment-relative offset of the segment peak loudness in seconds. Combined with loudness_start and
     * loudness_max, these components can be used to describe the “attack” of the segment.
     */
    @SerialName("loudness_max_time") val loudnessMaxTime: Float,

    /**
     * The offset loudness of the segment in decibels (dB). This value should be equivalent to the loudness_start of the
     * following segment.
     */
    @SerialName("loudness_end") val loudnessEnd: Float,

    /**
     * A “chroma” vector representing the pitch content of the segment, corresponding to the 12 pitch classes C, C#, D
     * to B, with values ranging from 0 to 1 that describe the relative dominance of every pitch in the chromatic scale.
     */
    val pitches: List<Float>,

    /**
     * Timbre is the quality of a musical note or sound that distinguishes different types of musical instruments, or
     * voices. Timbre vectors are best used in comparison with each other.
     */
    val timbre: List<Float>
)
