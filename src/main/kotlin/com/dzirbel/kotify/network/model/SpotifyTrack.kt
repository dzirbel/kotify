package com.dzirbel.kotify.network.model

import com.dzirbel.kotify.cache.CacheableObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shared properties for Track objects; see [SimplifiedSpotifyTrack] and [FullSpotifyTrack].
 */
interface SpotifyTrack : SpotifyObject {
    /** A link to the Web API endpoint providing full details of the track. */
    override val href: String?

    /** The Spotify ID for the track. */
    override val id: String?

    /** The name of the track. */
    override val name: String

    /** The object type: "track". */
    override val type: String

    /** The Spotify URI for the track. */
    override val uri: String?

    /**
     * The artists who performed the track. Each artist object includes a link in href to more detailed information
     * about the artist.
     */
    val artists: List<SimplifiedSpotifyArtist>

    /** A list of the countries in which the track can be played, identified by their ISO 3166-1 alpha-2 code. */
    val availableMarkets: List<String>?

    /** The disc number (usually 1 unless the album consists of more than one disc). */
    val discNumber: Int

    /** The track length in milliseconds. */
    val durationMs: Long

    /** Whether or not the track has explicit lyrics (true = yes it does; false = no it does not OR unknown). */
    val explicit: Boolean

    /** External URLs for this track. */
    val externalUrls: SpotifyExternalUrl

    /** Whether or not the track is from a local file. */
    val isLocal: Boolean

    /**
     * Part of the response when Track Relinking is applied. If true, the track is playable in the given market.
     * Otherwise false.
     */
    val isPlayable: Boolean?

    /**
     * Part of the response when Track Relinking is applied and is only part of the response if the track linking, in
     * fact, exists. The requested track has been replaced with a different track. The track in the linked_from object
     * contains information about the originally requested track.
     */
    val linkedFrom: SpotifyTrackLink?

    /** A URL to a 30 second preview (MP3 format) of the track. */
    val previewUrl: String?

    /**
     * Part of the response when Track Relinking is applied, the original track is not available in the given market,
     * and Spotify did not have any tracks to relink it with. The track response will still contain metadata for the
     * original track, and a restrictions object containing the reason why the track is not available:
     * "restrictions" : {"reason" : "market"}
     */
    val restrictions: Map<String, String>?

    /** The number of the track. If an album has several discs, the track number is the number on the specified disc. */
    val trackNumber: Int

    val album: SpotifyAlbum?
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-simplifiedtrackobject
 */
@Serializable
data class SimplifiedSpotifyTrack(
    override val artists: List<SimplifiedSpotifyArtist>,
    @SerialName("available_markets") override val availableMarkets: List<String>? = null,
    @SerialName("disc_number") override val discNumber: Int,
    @SerialName("duration_ms") override val durationMs: Long,
    override val explicit: Boolean,
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    override val href: String? = null,
    override val id: String? = null,
    @SerialName("is_local") override val isLocal: Boolean,
    @SerialName("is_playable") override val isPlayable: Boolean? = null,
    @SerialName("linked_from") override val linkedFrom: SpotifyTrackLink? = null,
    override val name: String,
    @SerialName("preview_url") override val previewUrl: String? = null,
    override val restrictions: Map<String, String>? = null,
    @SerialName("track_number") override val trackNumber: Int,
    override val type: String,
    override val uri: String? = null,

    /** Undocumented field. */
    override val album: SimplifiedSpotifyAlbum? = null,

    /** Undocumented field. */
    val episode: Boolean? = null,

    /** Undocumented field. */
    val track: Boolean? = null,

    /** Undocumented field. */
    @SerialName("external_ids")
    val externalIds: SpotifyExternalId? = null,

    /** Undocumented field. */
    val popularity: Int? = null
) : SpotifyTrack {
    override val cacheableObjects: Collection<CacheableObject>
        get() = album?.let { setOf(it) } ?: emptySet()
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-trackobject
 */
@Serializable
data class FullSpotifyTrack(
    override val artists: List<SimplifiedSpotifyArtist>,
    @SerialName("available_markets") override val availableMarkets: List<String>? = null,
    @SerialName("disc_number") override val discNumber: Int,
    @SerialName("duration_ms") override val durationMs: Long,
    override val explicit: Boolean,
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    override val href: String,
    override val id: String,
    @SerialName("is_local") override val isLocal: Boolean,
    @SerialName("is_playable") override val isPlayable: Boolean? = null,
    @SerialName("linked_from") override val linkedFrom: SpotifyTrackLink? = null,
    override val name: String,
    @SerialName("preview_url") override val previewUrl: String? = null,
    override val restrictions: Map<String, String>? = null,
    @SerialName("track_number") override val trackNumber: Int,
    override val type: String,
    override val uri: String,

    /**
     * The album on which the track appears. The album object includes a link in href to full information about the
     * album.
     */
    override val album: SimplifiedSpotifyAlbum,

    /** Known external IDs for the track. */
    @SerialName("external_ids") val externalIds: SpotifyExternalId,

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
    val popularity: Int
) : SpotifyTrack {
    override val cacheableObjects: Collection<CacheableObject>
        get() = setOf(album)
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-savedtrackobject
 */
@Serializable
data class SpotifySavedTrack(
    /**
     * The date and time the track was saved. Timestamps are returned in ISO 8601 format as Coordinated Universal Time
     * (UTC) with a zero offset: YYYY-MM-DDTHH:MM:SSZ. If the time is imprecise (for example, the date/time of an album
     * release), an additional field indicates the precision; see for example, release_date in an album object.
     */
    @SerialName("added_at") val addedAt: String,

    /** Information about the track. */
    val track: FullSpotifyTrack
) : CacheableObject {
    override val id: String? = null

    override val cacheableObjects: Collection<CacheableObject>
        get() = setOf(track)
}
