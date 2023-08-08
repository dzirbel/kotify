package com.dzirbel.kotify.network.model

import com.dzirbel.kotify.network.FullSpotifyTrackOrEpisode
import com.dzirbel.kotify.network.SimplifiedSpotifyTrackOrEpisode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface SpotifyEpisode : SpotifyObject {
    /** A link to the Web API endpoint providing full details of the episode. */
    override val href: String

    /** The Spotify ID for the episode. */
    override val id: String

    /** The name of the episode. */
    override val name: String

    /** The object type: "episode". */
    override val type: String

    /** The Spotify URI for the episode. */
    override val uri: String

    /** A URL to a 30 second preview (MP3 format) of the episode. null if not available. */
    val audioPreviewUrl: String?

    /** A description of the episode. */
    val description: String

    /** The episode length in milliseconds. */
    val durationMs: Long

    /** Whether or not the episode has explicit content (true = yes it does; false = no it does not OR unknown). */
    val explicit: Boolean

    /** External URLs for this episode. */
    val externalUrls: SpotifyExternalUrl

    /** Undocumented field */
    val htmlDescription: String?

    /** The cover art for the episode in various sizes, widest first. */
    val images: List<SpotifyImage>

    /** True if the episode is hosted outside of Spotify’s CDN. */
    val isExternallyHosted: Boolean

    /** True if the episode is playable in the given market. Otherwise false. */
    val isPlayable: Boolean

    /**
     * Note: This field is deprecated and might be removed in the future. Please use the languages field instead. The
     * language used in the episode, identified by a ISO 639 code.
     */
    val language: String?

    /** A list of the languages used in the episode, identified by their ISO 639 code. */
    val languages: List<String>

    /**
     * The date the episode was first released, for example "1981-12-15". Depending on the precision, it might be shown
     * as "1981" or "1981-12".
     */
    val releaseDate: String

    /** The precision with which release_date value is known: "year", "month", or "day". */
    val releaseDatePrecision: String

    /**
     * The user’s most recent position in the episode. Set if the supplied access token is a user token and has the
     * scope user-read-playback-position.
     */
    val resumePoint: SpotifyResumePoint?
}

@Serializable
data class SimplifiedSpotifyEpisode(
    override val href: String,
    override val id: String,
    override val name: String,
    override val type: String,
    override val uri: String,
    @SerialName("audio_preview_url") override val audioPreviewUrl: String? = null,
    override val description: String,
    @SerialName("duration_ms") override val durationMs: Long,
    override val explicit: Boolean,
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    @SerialName("html_description") override val htmlDescription: String?,
    override val images: List<SpotifyImage>,
    @SerialName("is_externally_hosted") override val isExternallyHosted: Boolean,
    @SerialName("is_playable") override val isPlayable: Boolean,
    override val language: String? = null,
    override val languages: List<String>,
    @SerialName("release_date") override val releaseDate: String,
    @SerialName("release_date_precision") override val releaseDatePrecision: String,
    @SerialName("resume_point") override val resumePoint: SpotifyResumePoint? = null,
) : SpotifyEpisode, SimplifiedSpotifyTrackOrEpisode

@Serializable
data class FullSpotifyEpisode(
    override val href: String,
    override val id: String,
    override val name: String,
    override val type: String,
    override val uri: String,
    @SerialName("audio_preview_url") override val audioPreviewUrl: String? = null,
    override val description: String,
    @SerialName("duration_ms") override val durationMs: Long,
    override val explicit: Boolean,
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    @SerialName("html_description") override val htmlDescription: String?,
    override val images: List<SpotifyImage>,
    @SerialName("is_externally_hosted") override val isExternallyHosted: Boolean,
    @SerialName("is_playable") override val isPlayable: Boolean,
    override val language: String? = null,
    override val languages: List<String>,
    @SerialName("release_date") override val releaseDate: String,
    @SerialName("release_date_precision") override val releaseDatePrecision: String,
    @SerialName("resume_point") override val resumePoint: SpotifyResumePoint? = null,

    /** The show on which the episode belongs. */
    val show: SimplifiedSpotifyShow,
) : SpotifyEpisode, FullSpotifyTrackOrEpisode
