package com.dominiczirbel.network.model

import com.google.gson.annotations.SerializedName

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#track-object-full
 */
data class FullTrack(
    /**
     * The album on which the track appears. The album object includes a link in href to full information about the
     * album.
     */
    val album: SimplifiedAlbum,

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
     * */
    @SerializedName("disc_number") val discNumber: Int,

    /**
     * The track length in milliseconds.
     */
    @SerializedName("duration_ms") val durationMs: Long,

    /**
     * Whether or not the track has explicit lyrics ( true = yes it does; false = no it does not OR unknown).
     */
    val explicit: Boolean,

    /**
     * Known external IDs for the track.
     */
    @SerializedName("external_ids") val externalIds: Map<String, String>,

    /**
     * Known external URLs for this track.
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
     * Part of the response when Track Relinking is applied, and the requested track has been replaced with different
     * track. The track in the linked_from object contains information about the originally requested track.
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
     * The popularity of the track. The value will be between 0 and 100, with 100 being the most popular.
     * The popularity of a track is a value between 0 and 100, with 100 being the most popular. The popularity is
     * calculated by algorithm and is based, in the most part, on the total number of plays the track has had and how
     * recent those plays are.
     * Generally speaking, songs that are being played a lot now will have a higher popularity than songs that were
     * played a lot in the past. Duplicate tracks (e.g. the same track from a single and an album) are rated
     * independently. Artist and album popularity is derived mathematically from track popularity. Note that the
     * popularity value may lag actual popularity by a few days: the value is not updated in real time.
     */
    val popularity: Int,

    /**
     * A link to a 30 second preview (MP3 format) of the track. Can be null
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
