package com.dzirbel.kotify.repository.player

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.dzirbel.kotify.network.FullSpotifyTrack
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.SpotifyTrackPlayback
import com.dzirbel.kotify.network.model.SpotifyTrackPlayback
import com.dzirbel.kotify.util.MockedTimeExtension
import com.dzirbel.kotify.util.collectingToList
import com.dzirbel.kotify.util.delayed
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tests for [PlayerRepository.refreshTrack], see [BasePlayerRepositoryTest].
 */
@ExtendWith(MockedTimeExtension::class)
class PlayerRepositoryRefreshTrackTest : BasePlayerRepositoryTest() {
    @Test
    fun refreshTrack() {
        val trackPlayback = SpotifyTrackPlayback(track = FullSpotifyTrack(), progressMs = 10)
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } delayed 1000 returns trackPlayback

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.assertTrackPlayback(refreshing = false, playback = null)

                repository.refreshTrack()

                repository.assertTrackPlayback(refreshing = false, playback = null)

                runCurrent()

                repository.assertTrackPlayback(refreshing = true, playback = null)

                advanceUntilIdle()

                repository.assertTrackPlayback(refreshing = false, playback = trackPlayback)
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentlyPlayingTrack() }
    }

    @Test
    fun refreshTrackCancelled() {
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } delayed 1000 throws CancellationException()

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.refreshTrack()

                advanceUntilIdle()

                repository.assertTrackPlayback(refreshing = false, playback = null)
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentlyPlayingTrack() }
    }

    @Test
    fun refreshTrackError() {
        val error = IOException()
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } delayed 1000 throws error

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.refreshTrack()

                advanceUntilIdle()

                repository.assertTrackPlayback(refreshing = false, playback = null)
                assertThat(errors).containsExactly(error)
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentlyPlayingTrack() }
    }

    @Test
    fun refreshTrackParallel() {
        val trackPlayback = SpotifyTrackPlayback(track = FullSpotifyTrack(), progressMs = 10)
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } delayed 1000 returns trackPlayback

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                launch { repository.refreshTrack() }
                launch { repository.refreshTrack() }

                advanceUntilIdle()

                repository.assertTrackPlayback(refreshing = false, playback = trackPlayback)
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentlyPlayingTrack() }
    }

    private fun PlayerRepository.assertTrackPlayback(refreshing: Boolean, playback: SpotifyTrackPlayback?) {
        assertThat(refreshingTrack.value).isEqualTo(refreshing)
        assertThat(currentItem.value).isEqualTo(playback?.item)

        if (playback?.progressMs != null) {
            val trackPosition = requireNotNull(trackPosition.value) as TrackPosition.Fetched
            assertThat(trackPosition.fetchedPositionMs.toLong()).isEqualTo(playback.progressMs)
        } else {
            assertThat(trackPosition.value).isNull()
        }

        assertThat(playing.value?.value).isEqualTo(playback?.isPlaying)
        assertThat(playbackContextUri.value).isEqualTo(playback?.context?.uri)
        assertThat(currentlyPlayingType.value).isEqualTo(playback?.currentlyPlayingType)
    }
}
