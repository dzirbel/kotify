package com.dominiczirbel.network.model

@Suppress("ComplexInterface")
interface Episode : SpotifyObject {
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
    val durationMs: Int

    /** Whether or not the episode has explicit content (true = yes it does; false = no it does not OR unknown). */
    val explicit: Boolean

    /** External URLs for this episode. */
    val externalUrls: ExternalUrls

    /** The cover art for the episode in various sizes, widest first. */
    val images: List<Image>

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
    val resumePoint: ResumePoint?
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#episode-object-simplified
 */
data class SimplifiedEpisode(
    override val href: String,
    override val id: String,
    override val name: String,
    override val type: String,
    override val uri: String,
    override val audioPreviewUrl: String?,
    override val description: String,
    override val durationMs: Int,
    override val explicit: Boolean,
    override val externalUrls: ExternalUrls,
    override val images: List<Image>,
    override val isExternallyHosted: Boolean,
    override val isPlayable: Boolean,
    override val language: String?,
    override val languages: List<String>,
    override val releaseDate: String,
    override val releaseDatePrecision: String,
    override val resumePoint: ResumePoint?
) : Episode

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#episode-object-full
 */
data class FullEpisode(
    override val href: String,
    override val id: String,
    override val name: String,
    override val type: String,
    override val uri: String,
    override val audioPreviewUrl: String?,
    override val description: String,
    override val durationMs: Int,
    override val explicit: Boolean,
    override val externalUrls: ExternalUrls,
    override val images: List<Image>,
    override val isExternallyHosted: Boolean,
    override val isPlayable: Boolean,
    override val language: String?,
    override val languages: List<String>,
    override val releaseDate: String,
    override val releaseDatePrecision: String,
    override val resumePoint: ResumePoint?,

    /** The show on which the episode belongs. */
    val show: SimplifiedShow
) : Episode
