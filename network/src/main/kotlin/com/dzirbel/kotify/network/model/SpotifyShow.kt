package com.dzirbel.kotify.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface SpotifyShow : SpotifyObject {
    /** A link to the Web API endpoint providing full details of the show. */
    override val href: String

    /** The Spotify ID for the show. */
    override val id: String

    /** The name of the show. */
    override val name: String

    /** The object type: “show”. */
    override val type: String

    /** The Spotify URI for the show. */
    override val uri: String

    /** A list of the countries in which the show can be played, identified by their ISO 3166-1 alpha-2 code. */
    val availableMarkets: List<String>

    /** The copyright statements of the show. */
    val copyrights: List<SpotifyCopyright>

    /** A description of the show. */
    val description: String

    /** Whether or not the show has explicit content (true = yes it does; false = no it does not OR unknown). */
    val explicit: Boolean

    /** Known external URLs for this show. */
    val externalUrls: SpotifyExternalUrl

    /** Undocumented field. */
    val htmlDescription: String?

    /** The cover art for the show in various sizes, widest first. */
    val images: List<SpotifyImage>

    /**
     * True if all of the show’s episodes are hosted outside of Spotify’s CDN. This field might be null in some cases.
     * */
    val isExternallyHosted: Boolean?

    /** A list of the languages used in the show, identified by their ISO 639 code. */
    val languages: List<String>

    /** The media type of the show. */
    val mediaType: String

    /** The publisher of the show. */
    val publisher: String

    /** Undocumented field. */
    val totalEpisodes: Int?
}

@Serializable
data class SimplifiedSpotifyShow(
    @SerialName("available_markets") override val availableMarkets: List<String>,
    override val copyrights: List<SpotifyCopyright>,
    override val description: String,
    override val explicit: Boolean,
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    override val href: String,
    @SerialName("html_description") override val htmlDescription: String?,
    override val id: String,
    override val images: List<SpotifyImage>,
    @SerialName("is_externally_hosted") override val isExternallyHosted: Boolean? = null,
    override val languages: List<String>,
    @SerialName("media_type") override val mediaType: String,
    override val name: String,
    override val publisher: String,
    @SerialName("total_episodes") override val totalEpisodes: Int,
    override val type: String,
    override val uri: String,
) : SpotifyShow

@Serializable
data class FullSpotifyShow(
    @SerialName("available_markets") override val availableMarkets: List<String>,
    override val copyrights: List<SpotifyCopyright>,
    override val description: String,
    override val explicit: Boolean,
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    override val href: String,
    @SerialName("html_description") override val htmlDescription: String?,
    override val id: String,
    override val images: List<SpotifyImage>,
    @SerialName("is_externally_hosted") override val isExternallyHosted: Boolean? = null,
    override val languages: List<String>,
    @SerialName("media_type") override val mediaType: String,
    override val name: String,
    override val publisher: String,
    @SerialName("total_episodes") override val totalEpisodes: Int,
    override val type: String,
    override val uri: String,

    val episodes: Paging<SimplifiedSpotifyEpisode>,
) : SpotifyShow

@Serializable
data class SpotifySavedShow(
    /**
     * The date and time the show was saved. Timestamps are returned in ISO 8601 format as Coordinated Universal Time
     * (UTC) with a zero offset: YYYY-MM-DDTHH:MM:SSZ. If the time is imprecise (for example, the date/time of an album
     * release), an additional field indicates the precision; see for example, release_date in an album object.
     */
    @SerialName("added_at") val addedAt: String,

    /** Information about the show. */
    val show: SimplifiedSpotifyShow,
)
