package com.dzirbel.kotify.ui.page.playlist

import com.dzirbel.kotify.repository.FakePlaylistRepository
import com.dzirbel.kotify.repository.FakePlaylistTracksRepository
import com.dzirbel.kotify.repository.FakeRatingRepository
import com.dzirbel.kotify.repository.FakeSavedPlaylistRepository
import com.dzirbel.kotify.repository.FakeSavedTrackRepository
import com.dzirbel.kotify.repository.FakeUserRepository
import com.dzirbel.kotify.repository.album.AlbumViewModel
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.playlist.PlaylistTrackViewModel
import com.dzirbel.kotify.repository.playlist.PlaylistViewModel
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.repository.track.TrackViewModel
import com.dzirbel.kotify.repository.user.UserViewModel
import com.dzirbel.kotify.repository.util.LazyTransactionStateFlow
import com.dzirbel.kotify.ui.ProvideFakeRepositories
import com.dzirbel.kotify.ui.page.FakeImageViewModel
import com.dzirbel.kotify.ui.page.render
import com.dzirbel.kotify.ui.themedScreenshotTest
import com.dzirbel.kotify.util.MockedTimeExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.GregorianCalendar
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@ExtendWith(MockedTimeExtension::class)
internal class PlaylistPageScreenshotTest {
    @Test
    fun empty() {
        themedScreenshotTest(filename = "empty") {
            ProvideFakeRepositories {
                PlaylistPage(playlistId = "playlistId").render()
            }
        }
    }

    @Test
    fun full() {
        val random = Random(0)

        val playlistId = "playlistId"
        val numTracks = 25
        val owner = UserViewModel(id = "user", name = "user")

        val playlist = PlaylistViewModel(
            id = playlistId,
            name = "Playlist",
            uri = playlistId,
            ownerId = owner.id,
            followersTotal = 10,
            totalTracks = numTracks,
            images = FakeImageViewModel(),
        )

        val tracks = List(numTracks) { index ->
            val artistId = index % 3
            val albumId = index % 5
            val track = TrackViewModel(
                id = "track-$index",
                name = "Track ${index + 1}",
                trackNumber = 0,
                durationMs = random.nextLong(5.minutes.inWholeMilliseconds),
                popularity = random.nextInt(100),
                artists = LazyTransactionStateFlow(
                    listOf(ArtistViewModel(id = artistId.toString(), name = "Artist ${artistId + 1}")),
                ),
                album = LazyTransactionStateFlow(
                    AlbumViewModel(id = albumId.toString(), name = "Album ${albumId + 1}"),
                ),
            )

            PlaylistTrackViewModel(
                track = track,
                indexOnPlaylist = index,
                addedAt = GregorianCalendar(2000, 0, index + 1).toInstant().toString(),
            )
        }

        val trackRatings = tracks
            .mapNotNull { playlistTrack ->
                playlistTrack.indexOnPlaylist.takeIf { it % 2 != 0 }?.let { it % 10 }?.let { rating ->
                    requireNotNull(playlistTrack.track).id to Rating(rating)
                }
            }
            .toMap()

        val trackSaveStates = trackRatings.mapValues { it.value.rating > 5 }

        themedScreenshotTest(filename = "full", windowWidth = 1500) {
            ProvideFakeRepositories(
                playlistRepository = FakePlaylistRepository(playlists = listOf(playlist)),
                playlistTracksRepository = FakePlaylistTracksRepository(playlistTracks = mapOf(playlistId to tracks)),
                savedPlaylistRepository = FakeSavedPlaylistRepository(savedStates = mapOf(playlistId to true)),
                savedTrackRepository = FakeSavedTrackRepository(savedStates = trackSaveStates),
                ratingRepository = FakeRatingRepository(ratings = trackRatings),
                userRepository = FakeUserRepository(users = listOf(owner)),
            ) {
                PlaylistPage(playlistId = playlistId).render()
            }
        }
    }
}
