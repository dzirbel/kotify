package com.dzirbel.kotify.ui.page.playlist

import com.dzirbel.kotify.db.DatabaseExtension
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.network.FullSpotifyPlaylist
import com.dzirbel.kotify.network.SimplifiedSpotifyAlbum
import com.dzirbel.kotify.network.SimplifiedSpotifyArtist
import com.dzirbel.kotify.network.SimplifiedSpotifyTrack
import com.dzirbel.kotify.repository.mockRatings
import com.dzirbel.kotify.repository.mockSaveState
import com.dzirbel.kotify.repository.mockSaveStates
import com.dzirbel.kotify.repository.mockStateCached
import com.dzirbel.kotify.repository.mockStateNull
import com.dzirbel.kotify.repository.playlist.PlaylistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistTrackViewModel
import com.dzirbel.kotify.repository.playlist.PlaylistTracksRepository
import com.dzirbel.kotify.repository.playlist.PlaylistViewModel
import com.dzirbel.kotify.repository.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository.rating.Rating
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import com.dzirbel.kotify.repository.user.UserRepository
import com.dzirbel.kotify.repository.user.UserViewModel
import com.dzirbel.kotify.ui.framework.render
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.ui.util.RelativeTimeInfo
import com.dzirbel.kotify.util.withMockedObjects
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.GregorianCalendar
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

@ExtendWith(DatabaseExtension::class)
internal class PlaylistPageScreenshotTest {
    @Test
    fun empty() {
        val playlistId = "playlistId"

        withMockedObjects(PlaylistRepository, PlaylistTracksRepository, SavedPlaylistRepository) {
            PlaylistRepository.mockStateNull(playlistId)
            PlaylistTracksRepository.mockStateNull(playlistId)
            SavedPlaylistRepository.mockSaveState(playlistId, saved = null)

            screenshotTest(filename = "empty") {
                PlaylistPage(playlistId = playlistId).render()
            }
        }
    }

    @Test
    fun full() {
        val random = Random(0)

        val playlistId = "playlistId"

        val networkPlaylist = FullSpotifyPlaylist(
            id = playlistId,
            name = "Playlist",
            tracks = List(25) { index ->
                val artistId = index % 3
                val albumId = index % 5
                SimplifiedSpotifyTrack(
                    id = "track-$index",
                    name = "Track ${index + 1}",
                    popularity = random.nextInt(100),
                    durationMs = random.nextLong(5.minutes.inWholeMilliseconds),
                    artists = listOf(
                        SimplifiedSpotifyArtist(id = artistId.toString(), name = "Artist ${artistId + 1}"),
                    ),
                    album = SimplifiedSpotifyAlbum(id = albumId.toString(), name = "Album ${albumId + 1}"),
                )
            },
            trackAddedAt = List(25) { index ->
                GregorianCalendar(2000, 0, index + 1).toInstant().toString()
            },
            followers = 10,
        )

        val playlist: Playlist
        val owner: User
        val tracks: List<PlaylistTrackViewModel>
        KotifyDatabase.blockingTransaction {
            playlist = PlaylistRepository.convertToDB(playlistId, networkPlaylist, fetchTime = Instant.now())
            owner = playlist.owner
            tracks = PlaylistTrack.tracksInOrder(playlistId).map { PlaylistTrackViewModel(it) }
        }

        RelativeTimeInfo.withMockedTime { now ->
            withMockedObjects(
                PlaylistRepository,
                PlaylistTracksRepository,
                SavedPlaylistRepository,
                TrackRatingRepository,
                SavedTrackRepository,
                UserRepository,
            ) {
                PlaylistRepository.mockStateCached(playlistId, PlaylistViewModel(playlist), now)
                PlaylistTracksRepository.mockStateCached(playlistId, tracks, now)
                SavedPlaylistRepository.mockSaveState(playlistId, saved = true)

                val trackIds = tracks.mapNotNull { it.track?.id }
                val ratings = tracks.map { track ->
                    track.indexOnPlaylist.takeIf { it % 2 != 0 }?.let { it % 10 }
                }

                TrackRatingRepository.mockRatings(
                    ids = trackIds,
                    ratings = ratings.map { rating -> rating?.let { Rating(rating, maxRating = 10) } },
                )

                SavedTrackRepository.mockSaveStates(
                    ids = trackIds,
                    saved = ratings.map { rating -> rating?.let { it > 5 } },
                )

                UserRepository.mockStateCached(id = owner.id.value, value = UserViewModel(owner), cacheTime = now)

                screenshotTest(filename = "full", windowWidth = 1500) {
                    PlaylistPage(playlistId = playlistId).render()
                }
            }
        }
    }
}
