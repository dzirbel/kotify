package com.dzirbel.kotify.repository.player

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.dzirbel.kotify.network.FullSpotifyTrack
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.SpotifyPlayback
import com.dzirbel.kotify.network.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.SpotifyTrackPlayback
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyTrackPlayback
import com.dzirbel.kotify.util.collectingToList
import com.dzirbel.kotify.util.delayed
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

// TODO finish test
class PlayerRepositoryTest {
    @Test
    fun refreshPlayback() {
        val playback = SpotifyPlayback(track = FullSpotifyTrack(), progressMs = 10)
        coEvery { Spotify.Player.getCurrentPlayback() } delayed 1000 returns playback

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.assertPlayback(refreshing = false, playback = null)

                repository.refreshPlayback()

                repository.assertPlayback(refreshing = false, playback = null)

                runCurrent()

                repository.assertPlayback(refreshing = true, playback = null)

                advanceUntilIdle()

                repository.assertPlayback(refreshing = false, playback = playback)
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentPlayback() }
    }

    @Test
    fun refreshPlaybackCancelled() {
        coEvery { Spotify.Player.getCurrentPlayback() } delayed 1000 throws CancellationException()

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.refreshPlayback()

                advanceUntilIdle()

                repository.assertPlayback(refreshing = false, playback = null)
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentPlayback() }
    }

    @Test
    fun refreshPlaybackError() {
        val error = IOException()
        coEvery { Spotify.Player.getCurrentPlayback() } delayed 1000 throws error

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.refreshPlayback()

                advanceUntilIdle()

                repository.assertPlayback(refreshing = false, playback = null)
                assertThat(errors).containsExactly(error)
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentPlayback() }
    }

    @Test
    fun refreshPlaybackParallel() {
        val playback = SpotifyPlayback(track = FullSpotifyTrack(), progressMs = 10)
        coEvery { Spotify.Player.getCurrentPlayback() } delayed 1000 returns playback

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                launch { repository.refreshPlayback() }
                launch { repository.refreshPlayback() }

                advanceUntilIdle()

                repository.assertPlayback(refreshing = false, playback = playback)
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentPlayback() }
    }

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

    @Test
    fun refreshDevices() {
        val devices = listOf(SpotifyPlaybackDevice(id = "1"), SpotifyPlaybackDevice(id = "2"))
        coEvery { Spotify.Player.getAvailableDevices() } delayed 1000 returns devices

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.assertDevices(refreshing = false, devices = null)

                repository.refreshDevices()

                repository.assertDevices(refreshing = false, devices = null)

                runCurrent()

                repository.assertDevices(refreshing = true, devices = null)

                advanceUntilIdle()

                repository.assertDevices(refreshing = false, devices = devices)
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getAvailableDevices() }
    }

    @Test
    fun refreshDevicesCancelled() {
        coEvery { Spotify.Player.getAvailableDevices() } delayed 1000 throws CancellationException()

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.refreshDevices()

                advanceUntilIdle()

                repository.assertDevices(refreshing = false, devices = null)
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getAvailableDevices() }
    }

    @Test
    fun refreshDevicesError() {
        val error = IOException()
        coEvery { Spotify.Player.getAvailableDevices() } delayed 1000 throws error

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                repository.refreshDevices()

                advanceUntilIdle()

                repository.assertDevices(refreshing = false, devices = null)
                assertThat(errors).containsExactly(error)
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getAvailableDevices() }
    }

    @Test
    fun refreshDevicesParallel() {
        val devices = listOf(SpotifyPlaybackDevice(id = "1"), SpotifyPlaybackDevice(id = "2"))
        coEvery { Spotify.Player.getAvailableDevices() } delayed 1000 returns devices

        runTest {
            val repository = PlayerRepository(scope = this)
            collectingToList(repository.errors) { errors ->
                launch { repository.refreshDevices() }
                launch { repository.refreshDevices() }

                advanceUntilIdle()

                repository.assertDevices(refreshing = false, devices = devices)
                assertThat(errors).isEmpty()
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getAvailableDevices() }
    }

    private fun PlayerRepository.assertPlayback(refreshing: Boolean, playback: SpotifyPlayback?) {
        assertThat(refreshingPlayback.value).isEqualTo(refreshing)
        assertThat(currentTrack.value).isEqualTo(playback?.item)
        assertThat(currentDevice.value).isEqualTo(playback?.device)

        if (playback?.progressMs != null) {
            val trackPosition = requireNotNull(trackPosition.value) as TrackPosition.Fetched
            assertThat(trackPosition.fetchedPositionMs.toLong()).isEqualTo(playback.progressMs)
        } else {
            assertThat(trackPosition.value).isNull()
        }

        assertThat(playing.value?.value).isEqualTo(playback?.isPlaying)
        assertThat(shuffling.value?.value).isEqualTo(playback?.shuffleState)
        assertThat(repeatMode.value?.value).isEqualTo(playback?.repeatState)
        assertThat(playbackContextUri.value).isEqualTo(playback?.context?.uri)
        assertThat(currentlyPlayingType.value).isEqualTo(playback?.currentlyPlayingType)

        assertThat(skipping.value).isEqualTo(
            when {
                playback?.actions?.skippingNext == true -> SkippingState.SKIPPING_TO_NEXT
                playback?.actions?.skippingPrev == true -> SkippingState.SKIPPING_TO_PREVIOUS
                else -> SkippingState.NOT_SKIPPING
            },
        )
    }

    private fun PlayerRepository.assertTrackPlayback(refreshing: Boolean, playback: SpotifyTrackPlayback?) {
        assertThat(refreshingTrack.value).isEqualTo(refreshing)
        assertThat(currentTrack.value).isEqualTo(playback?.item)

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

    private fun PlayerRepository.assertDevices(refreshing: Boolean, devices: List<SpotifyPlaybackDevice>?) {
        assertThat(refreshingDevices.value).isEqualTo(refreshing)
        assertThat(availableDevices.value).isEqualTo(devices)
    }
}
