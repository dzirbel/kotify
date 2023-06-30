package com.dzirbel.kotify.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface SpotifyUser : SpotifyObject {
    /** The Spotify user ID for this user. */
    override val id: String

    /** A link to the Web API endpoint for this user. */
    override val href: String

    /** The object type: "user" */
    override val type: String

    /** The Spotify URI for this user. */
    override val uri: String

    /** The name displayed on the user’s profile. null if not available. */
    val displayName: String?

    /** Known public external URLs for this user. */
    val externalUrls: SpotifyExternalUrl

    /** Information about the followers of this user. */
    val followers: SpotifyFollowers?

    /** The user’s profile image. */
    val images: List<SpotifyImage>?

    override val name: String
        get() = displayName.orEmpty()
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-publicuserobject
 */
@Serializable
data class PublicSpotifyUser(
    @SerialName("display_name") override val displayName: String? = null,
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    override val followers: SpotifyFollowers? = null,
    override val href: String,
    override val id: String,
    override val images: List<SpotifyImage>? = null,
    override val type: String,
    override val uri: String,
) : SpotifyUser

/**
 * https://developer.spotify.com/documentation/web-api/reference/#object-privateuserobject
 */
@Serializable
data class PrivateSpotifyUser(
    @SerialName("display_name") override val displayName: String? = null,
    @SerialName("external_urls") override val externalUrls: SpotifyExternalUrl,
    override val followers: SpotifyFollowers? = null,
    override val href: String,
    override val id: String,
    override val images: List<SpotifyImage>? = null,
    override val type: String,
    override val uri: String,

    /**
     * The country of the user, as set in the user’s account profile. An ISO 3166-1 alpha-2 country code. This field is
     * only available when the current user has granted access to the user-read-private scope.
     */
    val country: String? = null,

    /**
     * The user’s email address, as entered by the user when creating their account.
     * Important! This email address is unverified; there is no proof that it actually belongs to the user.
     * This field is only available when the current user has granted access to the user-read-email scope.
     */
    val email: String? = null,

    /**
     * The user’s explicit content settings. This field is only available when the current user has granted access to
     * the user-read-private scope.
     */
    @SerialName("explicit_content") val explicitContent: SpotifyExplicitContentSettings,

    /**
     * The user’s Spotify subscription level: "premium", "free", etc. (The subscription level "open" can be considered
     * the same as "free".)
     * This field is only available when the current user has granted access to the user-read-private scope.
     */
    val product: String? = null,
) : SpotifyUser
