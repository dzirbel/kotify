package com.dominiczirbel.network.model

import com.google.gson.annotations.SerializedName

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#track-object-simplified
 */
data class SimplifiedTrack(
    /**
     * The artists who performed the track. Each artist object includes a link in href to more detailed information
     * about the artist.
     */
    val artists: List<SimplifiedArtist>,

    /**
     * A list of the countries in which the track can be played, identified by their ISO 3166-1 alpha-2 code.
     */
    @SerializedName("available_markets") val availableMarkets: List<String>,

    /**
     * The disc number (usually 1 unless the album consists of more than one disc).
     */
    @SerializedName("disc_number") val discNumber: Int,

    /**
     * The track length in milliseconds.
     */
    @SerializedName("duration_ms") val durationMs: Long,

    /**
     * Whether or not the track has explicit lyrics (true = yes it does; false = no it does not OR unknown).
     */
    val explicit: Boolean,

    /**
     * External URLs for this track.
     */
    @SerializedName("external_urls") val externalUrls: Map<String, String>,

    /**
     * A link to the Web API endpoint providing full details of the track.
     */
    val href: String,

    /**
     * The Spotify ID for the track.
     */
    val id: String,

    /**
     * Part of the response when Track Relinking is applied. If true, the track is playable in the given market.
     * Otherwise false.
     */
    @SerializedName("is_playable") val isPlayable: Boolean?,

    /**
     * Part of the response when Track Relinking is applied and is only part of the response if the track linking, in
     * fact, exists. The requested track has been replaced with a different track. The track in the linked_from object
     * contains information about the originally requested track.
     */
    @SerializedName("linked_from") val linkedFrom: TrackLink?,

    /**
     * Part of the response when Track Relinking is applied, the original track is not available in the given market,
     * and Spotify did not have any tracks to relink it with. The track response will still contain metadata for the
     * original track, and a restrictions object containing the reason why the track is not available:
     * "restrictions" : {"reason" : "market"}
     */
    val restrictions: Map<String, String>?,

    /**
     * The name of the track.
     */
    val name: String,

    /**
     * A URL to a 30 second preview (MP3 format) of the track.
     */
    @SerializedName("preview_url") val previewUrl: String,

    /**
     * The number of the track. If an album has several discs, the track number is the number on the specified disc.
     */
    @SerializedName("track_number") val trackNumber: Int,

    /**
     * The object type: “track”.
     */
    val type: String,

    /**
     * The Spotify URI for the track.
     */
    val uri: String,

    /**
     * Whether or not the track is from a local file.
     */
    @SerializedName("is_local") val isLocal: Boolean
)
