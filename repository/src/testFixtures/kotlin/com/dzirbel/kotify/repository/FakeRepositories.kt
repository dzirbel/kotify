package com.dzirbel.kotify.repository

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistAlbum
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.network.model.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.model.SimplifiedSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyPlaylistTrack
import com.dzirbel.kotify.network.model.SpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyUser
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.album.AlbumViewModel
import com.dzirbel.kotify.repository.artist.ArtistAlbumViewModel
import com.dzirbel.kotify.repository.artist.ArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.playlist.PlaylistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistTrackViewModel
import com.dzirbel.kotify.repository.playlist.PlaylistTracksRepository
import com.dzirbel.kotify.repository.playlist.PlaylistViewModel
import com.dzirbel.kotify.repository.track.TrackRepository
import com.dzirbel.kotify.repository.track.TrackViewModel
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.user.UserViewModel
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import java.time.Instant

class FakeAlbumRepository(albums: Iterable<AlbumViewModel> = emptyList()) :
    FakeEntityRepository<AlbumViewModel, Album, SpotifyAlbum>(albums),
    AlbumRepository

class FakeArtistRepository(artists: Iterable<ArtistViewModel> = emptyList()) :
    FakeEntityRepository<ArtistViewModel, Artist, SpotifyArtist>(artists),
    ArtistRepository

class FakeArtistTracksRepository : ArtistTracksRepository {
    private val artistTracksStates = mutableMapOf<String, Set<String>?>()

    override fun setTrackArtists(trackId: String, artistIds: Iterable<String>) {
        artistTracksStates[trackId] = artistIds.toSet()
    }

    override fun artistTracksStateOf(artistId: String): StateFlow<Set<String>?> {
        return MutableStateFlow(artistTracksStates[artistId])
    }

    override fun artistTracksStatesOf(artistIds: Iterable<String>): List<StateFlow<Set<String>?>> {
        return artistIds.map { artistTracksStateOf(it) }
    }
}

class FakeArtistAlbumsRepository(artistAlbums: Map<String, List<ArtistAlbumViewModel>> = emptyMap()) :
    FakeRepository<List<ArtistAlbumViewModel>>(values = artistAlbums),
    ConvertingRepository<List<ArtistAlbum>, List<SimplifiedSpotifyAlbum>> by FakeConvertingRepository(),
    ArtistAlbumsRepository

class FakeAlbumTracksRepository(albumTracks: Map<String, List<TrackViewModel>> = emptyMap()) :
    FakeRepository<List<TrackViewModel>>(values = albumTracks),
    ConvertingRepository<List<Track>, List<SimplifiedSpotifyTrack>> by FakeConvertingRepository(),
    AlbumTracksRepository

class FakePlaylistRepository(playlists: Iterable<PlaylistViewModel> = emptyList()) :
    FakeEntityRepository<PlaylistViewModel, Playlist, SpotifyPlaylist>(playlists),
    PlaylistRepository

class FakePlaylistTracksRepository(playlistTracks: Map<String, List<PlaylistTrackViewModel>> = emptyMap()) :
    FakeRepository<List<PlaylistTrackViewModel>>(values = playlistTracks),
    ConvertingRepository<List<PlaylistTrack>, List<SpotifyPlaylistTrack>> by FakeConvertingRepository(),
    PlaylistTracksRepository {

    override fun reorder(
        playlistId: String,
        tracks: List<PlaylistTrackViewModel>,
        comparator: Comparator<PlaylistTrackViewModel>,
    ): Flow<PlaylistTracksRepository.PlaylistReorderState> {
        return emptyFlow()
    }

    override fun convertTrack(
        spotifyPlaylistTrack: SpotifyPlaylistTrack,
        playlistId: String,
        index: Int,
        fetchTime: Instant,
    ): PlaylistTrack? {
        return null
    }
}

class FakeTrackRepository(tracks: Iterable<TrackViewModel> = emptyList()) :
    FakeEntityRepository<TrackViewModel, Track, SpotifyTrack>(tracks),
    TrackRepository

class FakeUserRepository(
    currentUser: UserViewModel = UserViewModel(id = "kotify", name = "Kotify"),
    currentUserId: String = currentUser.id,
    users: Iterable<UserViewModel> = emptyList(),
) : FakeEntityRepository<UserViewModel, User, SpotifyUser>(users),
    UserRepository {

    override val currentUserId = MutableStateFlow(currentUserId)

    override val currentUser = MutableStateFlow(CacheState.Loaded(currentUser, CurrentTime.instant))

    override val hasCurrentUserId: Boolean = true

    override val requireCurrentUserId: String = currentUserId

    override fun onConnectToDatabase() {}
    override fun ensureCurrentUserLoaded() {}
    override fun signOut() {}
}
