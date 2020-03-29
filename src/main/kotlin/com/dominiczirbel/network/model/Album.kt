package com.dominiczirbel.network.model

import com.google.gson.annotations.SerializedName

@Suppress("ComplexInterface")
interface Album : SpotifyObject {
    /** A link to the Web API endpoint providing full details of the album. */
    override val href: String

    /** The Spotify ID for the album. */
    override val id: String

    /** The name of the album. In case of an album takedown, the value may be an empty string. */
    override val name: String

    /** The object type: “album” */
    override val type: String

    /** The Spotify URI for the album. */
    override val uri: String

    /** The type of the album: one of “album”, “single”, or “compilation”. */
    val albumType: Type

    /**
     * The artists of the album. Each artist object includes a link in href to more detailed information about the
     * artist.
     */
    val artists: List<SimplifiedArtist>

    /**
     * The markets in which the album is available: ISO 3166-1 alpha-2 country codes. Note that an album is considered
     * available in a market when at least 1 of its tracks is available in that market.
     */
    val availableMarkets: List<String>

    /** Known external URLs for this album. */
    val externalUrls: Map<String, String>

    /** The cover art for the album in various sizes, widest first. */
    val images: List<Image>

    /**
     * The date the album was first released, for example 1981. Depending on the precision, it might be shown as
     * 1981-12 or 1981-12-15.
     */
    val releaseDate: String

    /** The precision with which release_date value is known: year, month, or day. */
    val releaseDatePrecision: String

    /**
     * Part of the response when Track Relinking is applied, the original track is not available in the given market,
     * and Spotify did not have any tracks to relink it with. The track response will still contain metadata for the
     * original track, and a restrictions object containing the reason why the track is not available:
     * "restrictions" : {"reason" : "market"}
     */
    val restrictions: Map<String, String>?

    enum class Type {
        @SerializedName("album")
        ALBUM,

        @SerializedName("single")
        SINGLE,

        @SerializedName("appears_on")
        APPEARS_ON,

        @SerializedName("compilation")
        COMPILATION
    }
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#album-object-simplified
 * https://developer.spotify.com/documentation/web-api/reference-beta/#object-simplifiedalbumobject
 */
data class SimplifiedAlbum(
    override val albumType: Album.Type,
    override val artists: List<SimplifiedArtist>,
    override val availableMarkets: List<String>,
    override val externalUrls: Map<String, String>,
    override val href: String,
    override val id: String,
    override val images: List<Image>,
    override val name: String,
    override val releaseDate: String,
    override val releaseDatePrecision: String,
    override val restrictions: Map<String, String>?,
    override val type: String,
    override val uri: String,

    /**
     * The field is present when getting an artist’s albums. Possible values are “album”, “single”, “compilation”,
     * “appears_on”. Compare to album_type this field represents relationship between the artist and the album.
     */
    val albumGroup: Album.Type?
) : Album

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#album-object-full
 * https://developer.spotify.com/documentation/web-api/reference-beta/#object-albumobject
 */
data class FullAlbum(
    override val albumType: Album.Type,
    override val artists: List<SimplifiedArtist>,
    override val availableMarkets: List<String>,
    override val externalUrls: Map<String, String>,
    override val href: String,
    override val id: String,
    override val images: List<Image>,
    override val name: String,
    override val releaseDate: String,
    override val releaseDatePrecision: String,
    override val restrictions: Map<String, String>?,
    override val type: String,
    override val uri: String,

    /** The copyright statements of the album. */
    val copyrights: List<Copyright>,

    /** Known external IDs for the album. */
    val externalIds: ExternalId,

    /**
     * A list of the genres used to classify the album. For example: "Prog Rock" , "Post-Grunge". (If not yet
     * classified, the array is empty.)
     * */
    val genres: List<String>,

    /** The label for the album. */
    val label: String,

    /**
     * The popularity of the album. The value will be between 0 and 100, with 100 being the most popular. The popularity
     * is calculated from the popularity of the album’s individual tracks.
     */
    val popularity: Int,

    /** The tracks of the album. */
    val tracks: Paging<SimplifiedTrack>
) : Album
