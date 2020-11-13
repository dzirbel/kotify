package com.dominiczirbel.network.model

interface User {
    /** The name displayed on the user’s profile. null if not available. */
    val displayName: String

    /** Known public external URLs for this user. */
    val externalUrl: ExternalUrl

    /** Information about the followers of this user. */
    val followers: Followers

    /** A link to the Web API endpoint for this user. */
    val href: String

    /** The Spotify user ID for this user. */
    val id: String

    /** The user’s profile image. */
    val images: List<Image>

    /** The object type: “user” */
    val type: String

    /** The Spotify URI for this user. */
    val uri: String
}

data class PublicUser(
    override val displayName: String,
    override val externalUrl: ExternalUrl,
    override val followers: Followers,
    override val href: String,
    override val id: String,
    override val images: List<Image>,
    override val type: String,
    override val uri: String
) : User

data class PrivateUser(
    override val displayName: String,
    override val externalUrl: ExternalUrl,
    override val followers: Followers,
    override val href: String,
    override val id: String,
    override val images: List<Image>,
    override val type: String,
    override val uri: String,

    /**
     * The country of the user, as set in the user’s account profile. An ISO 3166-1 alpha-2 country code. This field is
     * only available when the current user has granted access to the user-read-private scope.
     */
    val country: String,

    /**
     * The user’s email address, as entered by the user when creating their account.
     * Important! This email address is unverified; there is no proof that it actually belongs to the user.
     * This field is only available when the current user has granted access to the user-read-email scope.
     */
    val email: String,

    /**
     * The user’s Spotify subscription level: “premium”, “free”, etc. (The subscription level “open” can be considered
     * the same as “free”.)
     * This field is only available when the current user has granted access to the user-read-private scope.
     */
    val product: String
) : User
