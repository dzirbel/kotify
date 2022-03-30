package com.dzirbel.kotify.db.model

import com.dzirbel.kotify.db.DatabaseRepository
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.ReadOnlyCachedProperty
import com.dzirbel.kotify.db.ReadWriteCachedProperty
import com.dzirbel.kotify.db.SavedDatabaseRepository
import com.dzirbel.kotify.db.SavedEntityTable
import com.dzirbel.kotify.db.SpotifyEntity
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
import com.dzirbel.kotify.util.mapParallel
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import java.time.Instant

object PlaylistTable : SpotifyEntityTable(name = "playlists") {
    private const val SNAPSHOT_ID_LENGTH = 128

    val collaborative: Column<Boolean> = bool("collaborative")
    val description: Column<String?> = text("description").nullable()
    val owner: Column<EntityID<String>> = reference("owner", UserTable)
    val public: Column<Boolean?> = bool("public").nullable()
    val snapshotId: Column<String> = varchar("snapshotId", length = SNAPSHOT_ID_LENGTH)
    val followersTotal: Column<Int?> = integer("followers_total").nullable()
    val totalTracks: Column<Int?> = integer("total_tracks").nullable()

    object PlaylistImageTable : Table() {
        val playlist = reference("playlist", PlaylistTable)
        val image = reference("image", ImageTable)
        override val primaryKey = PrimaryKey(playlist, image)
    }

    object SavedPlaylistsTable : SavedEntityTable(name = "saved_playlists")
}

class Playlist(id: EntityID<String>) : SpotifyEntity(id = id, table = PlaylistTable) {
    var collaborative: Boolean by PlaylistTable.collaborative
    var description: String? by PlaylistTable.description
    var public: Boolean? by PlaylistTable.public
    var snapshotId: String by PlaylistTable.snapshotId
    var followersTotal: Int? by PlaylistTable.followersTotal
    var totalTracks: Int? by PlaylistTable.totalTracks

    val owner: ReadWriteCachedProperty<User> by (User referencedOn PlaylistTable.owner).cached()

    val images: ReadWriteCachedProperty<List<Image>> by (Image via PlaylistTable.PlaylistImageTable).cachedAsList()
    val largestImage: ReadOnlyCachedProperty<Image?> by (Image via PlaylistTable.PlaylistImageTable)
        .cachedReadOnly { it.largest() }

    val playlistTracks: ReadOnlyCachedProperty<List<PlaylistTrack>> by
    (PlaylistTrack referrersOn PlaylistTrackTable.playlist)
        .cachedReadOnly(baseToDerived = { it.toList() })

    val playlistTracksInOrder: ReadOnlyCachedProperty<List<PlaylistTrack>> = ReadOnlyCachedProperty {
        PlaylistTrack.find { PlaylistTrackTable.playlist eq this@Playlist.id }
            .orderBy(PlaylistTrackTable.indexOnPlaylist to SortOrder.ASC)
            .toList()
    }

    val tracks: ReadOnlyCachedProperty<List<Track>> by (PlaylistTrack referrersOn PlaylistTrackTable.playlist)
        .cachedReadOnly(baseToDerived = { playlistTracks -> playlistTracks.map { it.track.live } })

    val hasAllTracks: Boolean
        get() = totalTracks?.let { playlistTracks.live.size == it } == true

    val hasAllTracksCached: Boolean
        get() = totalTracks?.let { (playlistTracks.cachedOrNull ?: tracks.cached).size == it } == true

    suspend fun getAllTracks(): List<PlaylistTrack> {
        val cachedTracks = KotifyDatabase.transaction("load tracks for playlist $name") {
            totalTracks?.let { totalTracks ->
                playlistTracksInOrder.live.takeIf { it.size == totalTracks }
            }
        }
        cachedTracks?.let { return it }

        val networkTracks = Spotify.Playlists.getPlaylistTracks(playlistId = id.value)
            .fetchAll<SpotifyPlaylistTrack>()

        return KotifyDatabase.transaction("save tracks for playlist $name") {
            networkTracks.mapIndexedNotNull { index, track ->
                PlaylistTrack.from(spotifyPlaylistTrack = track, playlist = this@Playlist, index = index)
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
                    totalTracks = it.total
                }
            }

            if (networkModel is FullSpotifyPlaylist) {
                fullUpdatedTime = Instant.now()
                followersTotal = networkModel.followers.total

                totalTracks = networkModel.tracks.total
                networkModel.tracks.items.mapIndexedNotNull { index, track ->
                    PlaylistTrack.from(spotifyPlaylistTrack = track, playlist = this, index = index)
                }
            }
        }
    }
}

object PlaylistRepository : DatabaseRepository<Playlist, SpotifyPlaylist>(Playlist) {
    override suspend fun fetch(id: String) = Spotify.Playlists.getPlaylist(playlistId = id)
}

object SavedPlaylistRepository : SavedDatabaseRepository<SpotifyPlaylist>(
    entityName = "playlist",
    savedEntityTable = PlaylistTable.SavedPlaylistsTable,
) {
    override suspend fun fetchIsSaved(ids: List<String>): List<Boolean> {
        val userId = requireNotNull(UserRepository.currentUserId.cached) { "no logged-in user" }

        return ids.mapParallel { id ->
            Spotify.Follow.isFollowingPlaylist(playlistId = id, userIds = listOf(userId))
                .first()
        }
    }

    override suspend fun pushSaved(ids: List<String>, saved: Boolean) {
        if (saved) {
            ids.mapParallel { id -> Spotify.Follow.followPlaylist(playlistId = id) }
        } else {
            ids.mapParallel { id -> Spotify.Follow.unfollowPlaylist(playlistId = id) }
        }
    }

    override suspend fun fetchLibrary(): Iterable<SpotifyPlaylist> {
        return Spotify.Playlists.getPlaylists(limit = Spotify.MAX_LIMIT)
            .fetchAll<SimplifiedSpotifyPlaylist>()
    }

    override fun from(savedNetworkType: SpotifyPlaylist): String? {
        return Playlist.from(savedNetworkType)?.id?.value
    }
}
