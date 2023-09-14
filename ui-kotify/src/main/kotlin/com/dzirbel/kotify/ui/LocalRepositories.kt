package com.dzirbel.kotify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.repository.album.AlbumRepository
import com.dzirbel.kotify.repository.album.AlbumTracksRepository
import com.dzirbel.kotify.repository.album.DatabaseAlbumRepository
import com.dzirbel.kotify.repository.album.DatabaseAlbumTracksRepository
import com.dzirbel.kotify.repository.album.DatabaseSavedAlbumRepository
import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.artist.ArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import com.dzirbel.kotify.repository.artist.DatabaseArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.DatabaseArtistRepository
import com.dzirbel.kotify.repository.artist.DatabaseArtistTracksRepository
import com.dzirbel.kotify.repository.artist.DatabaseSavedArtistRepository
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.repository.player.PlayerRepository
import com.dzirbel.kotify.repository.playlist.DatabasePlaylistRepository
import com.dzirbel.kotify.repository.playlist.DatabasePlaylistTracksRepository
import com.dzirbel.kotify.repository.playlist.DatabaseSavedPlaylistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistTracksRepository
import com.dzirbel.kotify.repository.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository.rating.DatabaseRatingRepository
import com.dzirbel.kotify.repository.rating.RatingRepository
import com.dzirbel.kotify.repository.track.DatabaseSavedTrackRepository
import com.dzirbel.kotify.repository.track.DatabaseTrackRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import com.dzirbel.kotify.repository.track.TrackRepository
import com.dzirbel.kotify.repository.user.DatabaseUserRepository
import com.dzirbel.kotify.repository.user.UserRepository
import kotlinx.coroutines.Dispatchers

val LocalArtistRepository = staticCompositionLocalOf<ArtistRepository> { error("not provided") }
val LocalArtistAlbumsRepository = staticCompositionLocalOf<ArtistAlbumsRepository> { error("not provided") }
val LocalArtistTracksRepository = staticCompositionLocalOf<ArtistTracksRepository> { error("not provided") }
val LocalAlbumRepository = staticCompositionLocalOf<AlbumRepository> { error("not provided") }
val LocalAlbumTracksRepository = staticCompositionLocalOf<AlbumTracksRepository> { error("not provided") }
val LocalPlaylistRepository = staticCompositionLocalOf<PlaylistRepository> { error("not provided") }
val LocalPlaylistTracksRepository = staticCompositionLocalOf<PlaylistTracksRepository> { error("not provided") }
val LocalTrackRepository = staticCompositionLocalOf<TrackRepository> { error("not provided") }
val LocalUserRepository = staticCompositionLocalOf<UserRepository> { error("not provided") }

val LocalSavedAlbumRepository = staticCompositionLocalOf<SavedAlbumRepository> { error("not provided") }
val LocalSavedArtistRepository = staticCompositionLocalOf<SavedArtistRepository> { error("not provided") }
val LocalSavedPlaylistRepository = staticCompositionLocalOf<SavedPlaylistRepository> { error("not provided") }
val LocalSavedTrackRepository = staticCompositionLocalOf<SavedTrackRepository> { error("not provided") }
val LocalSavedRepositories = staticCompositionLocalOf<List<SavedRepository>> { error("not provided") }

val LocalRatingRepository = staticCompositionLocalOf<RatingRepository> { error("not provided") }

val LocalPlayer = staticCompositionLocalOf<Player> { error("not provided") }

@Composable
fun ProvideRepositories(content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope { Dispatchers.IO }

    val artistRepository = DatabaseArtistRepository(scope)
    val artistTracksRepository = DatabaseArtistTracksRepository(scope)

    lateinit var albumRepository: AlbumRepository

    val trackRepository = DatabaseTrackRepository(
        scope = scope,
        albumRepository = lazy { albumRepository },
        artistRepository = artistRepository,
        artistTracksRepository = artistTracksRepository,
    )
    albumRepository = DatabaseAlbumRepository(scope, artistRepository, trackRepository)

    val artistAlbumsRepository = DatabaseArtistAlbumsRepository(scope, albumRepository)
    val albumTracksRepository = DatabaseAlbumTracksRepository(scope, trackRepository)

    lateinit var savedRepositories: List<SavedRepository>
    val userRepository = DatabaseUserRepository(
        applicationScope = scope,
        userSessionScope = Repository.userSessionScope,
        savedRepositories = lazy { savedRepositories },
    )

    val playlistTracksRepository = DatabasePlaylistTracksRepository(scope, trackRepository, userRepository)
    val playlistRepository = DatabasePlaylistRepository(scope, playlistTracksRepository, userRepository)

    val savedAlbumRepository = DatabaseSavedAlbumRepository(scope, userRepository, albumRepository)
    val savedArtistRepository = DatabaseSavedArtistRepository(scope, userRepository, artistRepository)
    val savedPlaylistRepository = DatabaseSavedPlaylistRepository(scope, userRepository, playlistRepository)
    val savedTrackRepository = DatabaseSavedTrackRepository(scope, userRepository, trackRepository)

    savedRepositories =
        listOf(savedAlbumRepository, savedArtistRepository, savedPlaylistRepository, savedTrackRepository)

    val ratingRepository = DatabaseRatingRepository(
        userRepository = userRepository,
        applicationScope = scope,
        userSessionScope = Repository.userSessionScope,
        artistTracksRepository = artistTracksRepository,
        albumTracksRepository = albumTracksRepository,
    )

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
        LocalSavedRepositories provides savedRepositories,

        LocalRatingRepository provides ratingRepository,

        LocalPlayer provides PlayerRepository(scope),

        content = content,
    )
}
