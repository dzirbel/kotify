package com.dominiczirbel.network.model

@Suppress("ComplexInterface")
interface Playlist : SpotifyObject {
    /** A link to the Web API endpoint providing full details of the playlist. */
    override val href: String

    /** The Spotify ID for the playlist. */
    override val id: String

    /** The name of the playlist. */
    override val name: String

    /** The object type: "playlist" */
    override val type: String

    /** The Spotify URI for the playlist. */
    override val uri: String

    /**
     * Returns true if context is not search and the owner allows other users to modify the playlist. Otherwise returns
     * false.
     */
    val collaborative: Boolean

    /** The playlist description. Only returned for modified, verified playlists, otherwise null. */
    val description: String

    /** Known external URLs for this playlist. */
    val externalUrls: ExternalUrl

    /**
     * Images for the playlist. The array may be empty or contain up to three images. The images are returned by size in
     * descending order. See Working with Playlists.
     * Note: If returned, the source URL for the image ( url ) is temporary and will expire in less than a day.
     */
    val images: List<Image>

    /** The user who owns the playlist */
    val owner: PublicUser

    /**
     * The playlist’s public/private status: true the playlist is public, false the playlist is private, null the
     * playlist status is not relevant. For more about public/private status, see Working with Playlists.
     */
    val public: Boolean?

    /**
     * The version identifier for the current playlist. Can be supplied in other requests to target a specific playlist
     * version
     */
    val snapshotId: String
}

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#playlist-object-simplified
 * TODO beta api
 */
data class SimplifiedPlaylist(
    override val href: String,
    override val id: String,
    override val name: String,
    override val type: String,
    override val uri: String,
    override val collaborative: Boolean,
    override val description: String,
    override val externalUrls: ExternalUrl,
    override val images: List<Image>,
    override val owner: PublicUser,
    override val public: Boolean?,
    override val snapshotId: String
) : Playlist

/**
 * https://developer.spotify.com/documentation/web-api/reference/object-model/#playlist-object-full
 * TODO beta api
 */
data class FullPlaylist(
    override val href: String,
    override val id: String,
    override val name: String,
    override val type: String,
    override val uri: String,
    override val collaborative: Boolean,
    override val description: String,
    override val externalUrls: ExternalUrl,
    override val images: List<Image>,
    override val owner: PublicUser,
    override val public: Boolean?,
    override val snapshotId: String,

    /** Information about the followers of the playlist. */
    val followers: Followers,

    /** Information about the tracks of the playlist. */
    val tracks: Paging<PlaylistTrack>
) : Playlist
