package com.dominiczirbel.network.model

interface Artist : SpotifyObject {
    /** A link to the Web API endpoint providing full details of the artist. */
    override val href: String

    /** The Spotify ID for the artist. */
    override val id: String

    /** The name of the artist. */
    override val name: String

    /** The object type: "artist". */
    override val type: String

    /** The Spotify URI for the artist. */
    override val uri: String

    /** Known external URLs for this artist. */
    val externalUrls: ExternalUrl
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#artist-object-simplified
 */
data class SimplifiedArtist(
    override val externalUrls: Map<String, String>,
    override val href: String,
    override val id: String,
    override val name: String,
    override val type: String,
    override val uri: String
) : Artist

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#artist-object-full
 * https://developer.spotify.com/documentation/web-api/reference-beta/#object-artistobject
 */
data class FullArtist(
    override val externalUrls: Map<String, String>,
    override val href: String,
    override val id: String,
    override val name: String,
    override val type: String,
    override val uri: String,

    /** Information about the followers of the artist. */
    val followers: Followers,

    /**
     * A list of the genres the artist is associated with. For example: "Prog Rock" , "Post-Grunge". (If not yet
     * classified, the array is empty.)
     */
    val genres: List<String>,

    /** Images of the artist in various sizes, widest first. */
    val images: List<Image>,

    /**
     * The popularity of the artist. The value will be between 0 and 100, with 100 being the most popular. The artist’s
     * popularity is calculated from the popularity of all the artist’s tracks.
     */
    val popularity: Int
) : Artist
