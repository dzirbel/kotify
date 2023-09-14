package com.dzirbel.kotify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.dzirbel.kotify.repository.FakeAlbumRepository
import com.dzirbel.kotify.repository.FakeAlbumTracksRepository
import com.dzirbel.kotify.repository.FakeArtistAlbumsRepository
import com.dzirbel.kotify.repository.FakeArtistRepository
import com.dzirbel.kotify.repository.FakeArtistTracksRepository
import com.dzirbel.kotify.repository.FakePlayer
import com.dzirbel.kotify.repository.FakePlaylistRepository
import com.dzirbel.kotify.repository.FakePlaylistTracksRepository
import com.dzirbel.kotify.repository.FakeRatingRepository
import com.dzirbel.kotify.repository.FakeSavedAlbumRepository
import com.dzirbel.kotify.repository.FakeSavedArtistRepository
import com.dzirbel.kotify.repository.FakeSavedPlaylistRepository
import com.dzirbel.kotify.repository.FakeSavedTrackRepository
import com.dzirbel.kotify.repository.FakeTrackRepository
import com.dzirbel.kotify.repository.FakeUserRepository
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.artist.ArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.repository.playlist.PlaylistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistTracksRepository
import com.dzirbel.kotify.repository.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository.rating.RatingRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import com.dzirbel.kotify.repository.track.TrackRepository
import com.dzirbel.kotify.repository.user.UserRepository

@Composable
fun ProvideFakeRepositories(
    artistRepository: ArtistRepository = FakeArtistRepository(),
    artistAlbumsRepository: ArtistAlbumsRepository = FakeArtistAlbumsRepository(),
    artistTracksRepository: ArtistTracksRepository = FakeArtistTracksRepository(),
    albumRepository: AlbumRepository = FakeAlbumRepository(),
    albumTracksRepository: AlbumTracksRepository = FakeAlbumTracksRepository(),
    playlistRepository: PlaylistRepository = FakePlaylistRepository(),
    playlistTracksRepository: PlaylistTracksRepository = FakePlaylistTracksRepository(),
    trackRepository: TrackRepository = FakeTrackRepository(),
    userRepository: UserRepository = FakeUserRepository(),
    savedAlbumRepository: SavedAlbumRepository = FakeSavedAlbumRepository(),
    savedArtistRepository: SavedArtistRepository = FakeSavedArtistRepository(),
    savedPlaylistRepository: SavedPlaylistRepository = FakeSavedPlaylistRepository(),
    savedTrackRepository: SavedTrackRepository = FakeSavedTrackRepository(),
    ratingRepository: RatingRepository = FakeRatingRepository(),
    player: Player = FakePlayer(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalArtistRepository provides artistRepository,
        LocalArtistAlbumsRepository provides artistAlbumsRepository,
        LocalArtistTracksRepository provides artistTracksRepository,
        LocalAlbumRepository provides albumRepository,
        LocalAlbumTracksRepository provides albumTracksRepository,
        LocalPlaylistRepository provides playlistRepository,
        LocalPlaylistTracksRepository provides playlistTracksRepository,
        LocalTrackRepository provides trackRepository,
        LocalUserRepository provides userRepository,

        LocalSavedAlbumRepository provides savedAlbumRepository,
        LocalSavedArtistRepository provides savedArtistRepository,
        LocalSavedPlaylistRepository provides savedPlaylistRepository,
        LocalSavedTrackRepository provides savedTrackRepository,
        LocalSavedRepositories provides
            listOf(savedAlbumRepository, savedArtistRepository, savedPlaylistRepository, savedTrackRepository),

        LocalRatingRepository provides ratingRepository,

        LocalPlayer provides player,

        content = content,
    )
}
