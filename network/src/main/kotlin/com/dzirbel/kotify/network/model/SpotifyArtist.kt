package com.dzirbel.kotify.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface SpotifyArtist : SpotifyObject {
    /** A link to the Web API endpoint providing full details of the artist. */
    override val href: String?

    /** The Spotify ID for the artist. */
    override val id: String?

    /** The name of the artist. */
    override val name: String

    /** The object type: "artist". */
    override val type: String

    /** The Spotify URI for the artist. */
    override val uri: String?

    /** Known external URLs for this artist. */
    val externalUrls: SpotifyExternalUrl
}

@Serializable
data class SimplifiedSpotifyArtist(
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    override val href: String? = null,
    override val id: String? = null,
    override val name: String,
    override val type: String,
    override val uri: String? = null,
) : SpotifyArtist

@Serializable
data class FullSpotifyArtist(
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    override val href: String,
    override val id: String,
    override val name: String,
    override val type: String,
    override val uri: String,

    /** Information about the followers of the artist. */
    val followers: SpotifyFollowers,

    /**
     * A list of the genres the artist is associated with. For example: "Prog Rock" , "Post-Grunge". (If not yet
     * classified, the array is empty.)
     */
    val genres: List<String>,

    /** Images of the artist in various sizes, widest first. */
    val images: List<SpotifyImage>,

    /**
     * The popularity of the artist. The value will be between 0 and 100, with 100 being the most popular. The artist’s
     * popularity is calculated from the popularity of all the artist’s tracks.
     */
    val popularity: Int,
) : SpotifyArtist
