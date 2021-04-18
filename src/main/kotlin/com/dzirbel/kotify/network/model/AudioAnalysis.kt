package com.dzirbel.kotify.network.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AudioAnalysis(
    /**
     * The time intervals of the bars throughout the track. A bar (or measure) is a segment of time defined as a given
     * number of beats. Bar offsets also indicate downbeats, the first beat of the measure.
     */
    val bars: List<TimeInterval>,

    /**
     * The time intervals of beats throughout the track. A beat is the basic time unit of a piece of music; for example,
     * each tick of a metronome. Beats are typically multiples of tatums.
     */
    val beats: List<TimeInterval>,

    /** Undocumented field. */
    val meta: JsonObject,

    /**
     * Sections are defined by large variations in rhythm or timbre, e.g. chorus, verse, bridge, guitar solo, etc. Each
     * section contains its own descriptions of tempo, key, mode, time_signature, and loudness.
     */
    val sections: List<Section>,

    /**
     * Audio segments attempts to subdivide a song into many segments, with each segment containing a roughly consistent
     * sound throughout its duration.
     */
    val segments: List<Segment>,

    /**
     * A tatum represents the lowest regular pulse train that a listener intuitively infers from the timing of perceived
     * musical events (segments). For more information about tatums, see Rhythm (below).
     */
    val tatums: List<TimeInterval>,

    /** Undocumented field. */
    val track: JsonObject? = null
)
