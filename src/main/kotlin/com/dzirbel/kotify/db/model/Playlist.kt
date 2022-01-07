package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.CachedProperty
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.ReadOnlyCachedProperty
import com.dzirbel.kotify.db.Repository
import com.dzirbel.kotify.db.SavableSpotifyEntity
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.db.SavedRepository
import com.dzirbel.kotify.db.SpotifyEntityClass
import com.dzirbel.kotify.db.SpotifyEntityTable
import com.dzirbel.kotify.db.cached
import com.dzirbel.kotify.db.cachedAsList
import com.dzirbel.kotify.db.cachedReadOnly
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyPlaylist
import com.dzirbel.kotify.network.model.SimplifiedSpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.time.Instant

object PlaylistTable : SpotifyEntityTable(name = "playlists") {
    val collaborative: Column<Boolean> = bool("collaborative")
    val description: Column<String?> = text("description").nullable()
    val owner: Column<EntityID<String>> = reference("owner", UserTable)
    val public: Column<Boolean?> = bool("public").nullable()
    val snapshotId: Column<String> = varchar("snapshotId", length = STRING_ID_LENGTH)
    val followersTotal: Column<UInt?> = uinteger("followers_total").nullable()
    val totalTracks: Column<UInt?> = uinteger("total_tracks").nullable()

    object PlaylistImageTable : Table() {
        val playlist = reference("playlist", PlaylistTable)
        val image = reference("image", ImageTable)
        override val primaryKey = PrimaryKey(playlist, image)
    }

    object SavedPlaylistsTable : SavedEntityTable(name = "saved_playlists")
}

class Playlist(id: EntityID<String>) : SavableSpotifyEntity(
    id = id,
    table = PlaylistTable,
    savedEntityTable = PlaylistTable.SavedPlaylistsTable,
) {
    var collaborative: Boolean by PlaylistTable.collaborative
    var description: String? by PlaylistTable.description
    var public: Boolean? by PlaylistTable.public
    var snapshotId: String by PlaylistTable.snapshotId
    var followersTotal: UInt? by PlaylistTable.followersTotal
    var totalTracks: UInt? by PlaylistTable.totalTracks

    val owner: CachedProperty<User> by (User referencedOn PlaylistTable.owner).cached()

    val images: CachedProperty<List<Image>> by (Image via PlaylistTable.PlaylistImageTable).cachedAsList()
    val playlistTracks: CachedProperty<List<PlaylistTrack>> by (PlaylistTrack via PlaylistTrackTable).cachedAsList()
    val tracks: ReadOnlyCachedProperty<List<Track>> by (PlaylistTrack via PlaylistTrackTable).cachedReadOnly(
        baseToDerived = { playlistTracks -> playlistTracks.map { it.track.live } },
    )

    val hasAllTracks: Boolean
        get() = totalTracks?.let { playlistTracks.live.size.toUInt() == it } == true

    suspend fun getAllTracks(): List<PlaylistTrack> {
        return if (hasAllTracks) {
            playlistTracks.live
        } else {
            val networkTracks = Spotify.Playlists.getPlaylistTracks(playlistId = id.value)
                .fetchAll<SpotifyPlaylistTrack>()

            KotifyDatabase.transaction {
                networkTracks.mapNotNull { PlaylistTrack.from(spotifyPlaylistTrack = it, playlist = this@Playlist) }
                    .also { playlistTracks.set(it) }
            }
        }
    }

    companion object : SpotifyEntityClass<Playlist, SpotifyPlaylist>(PlaylistTable) {
        override fun Playlist.update(networkModel: SpotifyPlaylist) {
            collaborative = networkModel.collaborative
            networkModel.description?.let { description = it }
            networkModel.public?.let { public = it }
            snapshotId = networkModel.snapshotId

            User.from(networkModel.owner)?.let { owner.set(it) }

            images.set(networkModel.images.map { Image.from(it) })

            if (networkModel is SimplifiedSpotifyPlaylist) {
                networkModel.tracks?.let {
                    totalTracks = it.total.toUInt()
                }
            }

            if (networkModel is FullSpotifyPlaylist) {
                fullUpdatedTime = Instant.now()
                followersTotal = networkModel.followers.total.toUInt()

                totalTracks = networkModel.tracks.total.toUInt()
                playlistTracks.set(
                    networkModel.tracks.items.mapNotNull {
                        PlaylistTrack.from(spotifyPlaylistTrack = it, playlist = this)
                    }
                )
            }
        }
    }
}

object PlaylistRepository : Repository<Playlist, SpotifyPlaylist>(Playlist) {
    override suspend fun fetch(id: String) = Spotify.Playlists.getPlaylist(playlistId = id)
}

object SavedPlaylistRepository : SavedRepository<SpotifyPlaylist>(
    savedEntityTable = PlaylistTable.SavedPlaylistsTable,
) {
    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        val userId = requireNotNull(
            KotifyDatabase.transaction { UserRepository.getCurrentUserIdCached() }
        ) { "no logged-in user" }

        // TODO fetch in parallel
        return ids.map { id ->
            Spotify.Follow.isFollowingPlaylist(playlistId = id, userIds = listOf(userId))
                .first()
        }
    }

    override suspend fun pushSaved(ids: List<String>, saved: Boolean) {
        // TODO push in parallel
        if (saved) {
            ids.forEach { id -> Spotify.Follow.followPlaylist(playlistId = id) }
        } else {
            ids.forEach { id -> Spotify.Follow.unfollowPlaylist(playlistId = id) }
        }
    }

    override suspend fun fetchLibrary(): Iterable<SpotifyPlaylist> {
        return Spotify.Playlists.getPlaylists(limit = Spotify.MAX_LIMIT)
            .fetchAll<SimplifiedSpotifyPlaylist>()
    }

    override fun from(savedNetworkType: SpotifyPlaylist): String? {
        return Playlist.from(networkModel = savedNetworkType)?.id?.value
    }
}
