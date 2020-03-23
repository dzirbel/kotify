package com.dominiczirbel.network.model

import com.google.gson.annotations.SerializedName

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#album-object-simplified
 */
data class SimplifiedAlbum(
    /**
     * The field is present when getting an artist’s albums. Possible values are “album”, “single”, “compilation”,
     * “appears_on”. Compare to album_type this field represents relationship between the artist and the album.
     */
    @SerializedName("album_group") val albumGroup: String?,

    /**
     * The type of the album: one of “album”, “single”, or “compilation”.
     */
    @SerializedName("album_type") val albumType: String,

    /**
     * The artists of the album. Each artist object includes a link in href to more detailed information about the
     * artist.
     */
    val artists: List<SimplifiedArtist>,

    /**
     * The markets in which the album is available: ISO 3166-1 alpha-2 country codes. Note that an album is considered
     * available in a market when at least 1 of its tracks is available in that market.
     */
    @SerializedName("available_markets") val availableMarkets: List<String>,

    /**
     * Known external URLs for this album.
     */
    @SerializedName("external_urls") val externalUrls: Map<String, String>,

    /**
     * A link to the Web API endpoint providing full details of the album.
     */
    val href: String,

    /**
     * The Spotify ID for the album.
     */
    val id: String,

    /**
     * The cover art for the album in various sizes, widest first.
     */
    val images: List<Image>,

    /**
     * The name of the album. In case of an album takedown, the value may be an empty string.
     */
    val name: String,

    /**
     * The date the album was first released, for example 1981. Depending on the precision, it might be shown as
     * 1981-12 or 1981-12-15.
     */
    @SerializedName("release_date") val releaseDate: String,

    /**
     * The precision with which release_date value is known: year, month, or day.
     */
    @SerializedName("release_date_precision") val releaseDatePrecision: String,

    /**
     * Part of the response when Track Relinking is applied, the original track is not available in the given market,
     * and Spotify did not have any tracks to relink it with. The track response will still contain metadata for the
     * original track, and a restrictions object containing the reason why the track is not available:
     * "restrictions" : {"reason" : "market"}
     */
    val restrictions: Map<String, String>?,

    /**
     * The object type: “album”
     */
    val type: String,

    /**
     * The Spotify URI for the album.
     */
    val uri: String
)
