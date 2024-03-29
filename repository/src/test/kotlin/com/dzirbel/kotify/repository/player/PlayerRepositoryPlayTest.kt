package com.dzirbel.kotify.repository.player

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.network.FullSpotifyTrack
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.SpotifyPlaybackContext
import com.dzirbel.kotify.network.SpotifyTrackPlayback
import com.dzirbel.kotify.repository.util.ToggleableState
import com.dzirbel.kotify.util.MockedTimeExtension
import com.dzirbel.kotify.util.collectingToList
import com.dzirbel.kotify.util.delayed
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Tests for [PlayerRepository.play], see [BasePlayerRepositoryTest].
 */
@ExtendWith(MockedTimeExtension::class)
class PlayerRepositoryPlayTest : BasePlayerRepositoryTest() {
    @Test
    fun playNullContext() {
        val track = FullSpotifyTrack(durationMs = 10_000)
        val trackPlayback1 = SpotifyTrackPlayback(track = track, progressMs = 10, isPlaying = false)
        val trackPlayback2 = SpotifyTrackPlayback(track = track, progressMs = 10, isPlaying = true)
        val trackPlayback3 = SpotifyTrackPlayback(track = track, progressMs = 5_000, isPlaying = false)
        coEvery { Spotify.Player.startPlayback() } just Runs
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() }
            .delayed(1000)
            .returnsMany(trackPlayback1, trackPlayback2, trackPlayback3)

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.play(context = null)

                advanceTimeBy(2500)

                assertThat(repository.playing.value).isEqualTo(ToggleableState.Set(true))
                assertThat(repository.currentItem.value).isEqualTo(track)
                assertThat(repository.trackPosition.value)
                    .transform { (it as TrackPosition.Fetched).fetchedPositionMs }
                    .isEqualTo(10)

                coVerify(exactly = 2) { Spotify.Player.getCurrentlyPlayingTrack() }

                advanceTimeBy(10_000)

                // refresh track after it is finished
                coVerify(exactly = 3) { Spotify.Player.getCurrentlyPlayingTrack() }
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.startPlayback() }
        coVerify(exactly = 3) { Spotify.Player.getCurrentlyPlayingTrack() }
    }

    @Test
    fun playNullContextWithTrackPosition() {
        val track = FullSpotifyTrack(durationMs = 10_000)
        val trackPlayback1 = SpotifyTrackPlayback(track = track, progressMs = 10, isPlaying = false)
        val trackPlayback2 = SpotifyTrackPlayback(track = track, progressMs = 10, isPlaying = true)
        val trackPlayback3 = SpotifyTrackPlayback(track = track, progressMs = 5_000, isPlaying = false)
        coEvery { Spotify.Player.startPlayback() } just Runs
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() }
            .delayed(1000)
            .returnsMany(trackPlayback1, trackPlayback1, trackPlayback2, trackPlayback3)

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.refreshTrack()
                advanceTimeBy(2500)
                assertThat((repository.trackPosition.value as TrackPosition.Fetched).playing).isEqualTo(false)
                assertThat((repository.trackPosition.value as TrackPosition.Fetched).fetchedPositionMs).isEqualTo(10)

                repository.play(context = null)

                advanceTimeBy(2500)

                assertThat(repository.playing.value).isEqualTo(ToggleableState.Set(true))
                assertThat((repository.trackPosition.value as TrackPosition.Fetched).playing).isEqualTo(true)
                assertThat((repository.trackPosition.value as TrackPosition.Fetched).fetchedPositionMs).isEqualTo(10)
                coVerify(exactly = 3) { Spotify.Player.getCurrentlyPlayingTrack() }

                advanceTimeBy(10_000)

                coVerify(exactly = 4) { Spotify.Player.getCurrentlyPlayingTrack() }
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.startPlayback() }
    }

    @Test
    fun playWithContext() {
        val oldContext = SpotifyPlaybackContext(uri = "spotify:album:old")
        val newContext = SpotifyPlaybackContext(uri = "spotify:album:new")
        val trackPlayback1 = SpotifyTrackPlayback(context = oldContext, isPlaying = true)
        val trackPlayback2 = SpotifyTrackPlayback(context = newContext, isPlaying = true)
        coEvery { Spotify.Player.startPlayback(contextUri = any()) } just Runs
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() }
            .delayed(1000)
            .returnsMany(trackPlayback1, trackPlayback2)

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.play(context = Player.PlayContext(contextUri = newContext.uri))

                advanceUntilIdle()

                assertThat(repository.playing.value).isEqualTo(ToggleableState.Set(true))
                assertThat(repository.playbackContextUri.value).isEqualTo(newContext.uri)

                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.startPlayback(contextUri = newContext.uri) }
        coVerify(exactly = 2) { Spotify.Player.getCurrentlyPlayingTrack() }
    }
}
