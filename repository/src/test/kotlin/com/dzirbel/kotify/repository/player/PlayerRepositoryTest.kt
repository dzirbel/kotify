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
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.util.collectingToList
import com.dzirbel.kotify.util.delayed
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

// TODO finish test
class PlayerRepositoryTest {
    @AfterEach
    fun cleanup() {
        PlayerRepository.clear()
    }

    @Test
    fun refreshPlayback() {
        val playback = SpotifyPlayback(track = FullSpotifyTrack(), progressMs = 10)
        coEvery { Spotify.Player.getCurrentPlayback() } delayed 1000 returns playback

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    assertPlayback(refreshing = false, playback = null)

                    PlayerRepository.refreshPlayback()

                    assertPlayback(refreshing = false, playback = null)

                    runCurrent()

                    assertPlayback(refreshing = true, playback = null)

                    advanceUntilIdle()

                    assertPlayback(refreshing = false, playback = playback)
                    assertThat(errors).isEmpty()
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentPlayback() }
    }

    @Test
    fun refreshPlaybackCancelled() {
        coEvery { Spotify.Player.getCurrentPlayback() } delayed 1000 throws CancellationException()

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    PlayerRepository.refreshPlayback()

                    advanceUntilIdle()

                    assertPlayback(refreshing = false, playback = null)
                    assertThat(errors).isEmpty()
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentPlayback() }
    }

    @Test
    fun refreshPlaybackError() {
        val error = IOException()
        coEvery { Spotify.Player.getCurrentPlayback() } delayed 1000 throws error

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    PlayerRepository.refreshPlayback()

                    advanceUntilIdle()

                    assertPlayback(refreshing = false, playback = null)
                    assertThat(errors).containsExactly(error)
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentPlayback() }
    }

    @Test
    fun refreshPlaybackParallel() {
        val playback = SpotifyPlayback(track = FullSpotifyTrack(), progressMs = 10)
        coEvery { Spotify.Player.getCurrentPlayback() } delayed 1000 returns playback

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    launch {
                        PlayerRepository.refreshPlayback()
                    }

                    launch {
                        PlayerRepository.refreshPlayback()
                    }

                    advanceUntilIdle()

                    assertPlayback(refreshing = false, playback = playback)
                    assertThat(errors).isEmpty()
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentPlayback() }
    }

    @Test
    fun refreshTrack() {
        val trackPlayback = SpotifyTrackPlayback(track = FullSpotifyTrack(), progressMs = 10)
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } delayed 1000 returns trackPlayback

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    assertTrackPlayback(refreshing = false, playback = null)

                    PlayerRepository.refreshTrack()

                    assertTrackPlayback(refreshing = false, playback = null)

                    runCurrent()

                    assertTrackPlayback(refreshing = true, playback = null)

                    advanceUntilIdle()

                    assertTrackPlayback(refreshing = false, playback = trackPlayback)
                    assertThat(errors).isEmpty()
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentlyPlayingTrack() }
    }

    @Test
    fun refreshTrackCancelled() {
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } delayed 1000 throws CancellationException()

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    PlayerRepository.refreshTrack()

                    advanceUntilIdle()

                    assertTrackPlayback(refreshing = false, playback = null)
                    assertThat(errors).isEmpty()
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentlyPlayingTrack() }
    }

    @Test
    fun refreshTrackError() {
        val error = IOException()
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } delayed 1000 throws error

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    PlayerRepository.refreshTrack()

                    advanceUntilIdle()

                    assertTrackPlayback(refreshing = false, playback = null)
                    assertThat(errors).containsExactly(error)
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentlyPlayingTrack() }
    }

    @Test
    fun refreshTrackParallel() {
        val trackPlayback = SpotifyTrackPlayback(track = FullSpotifyTrack(), progressMs = 10)
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } delayed 1000 returns trackPlayback

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    launch {
                        PlayerRepository.refreshTrack()
                    }

                    launch {
                        PlayerRepository.refreshTrack()
                    }

                    advanceUntilIdle()

                    assertTrackPlayback(refreshing = false, playback = trackPlayback)
                    assertThat(errors).isEmpty()
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getCurrentlyPlayingTrack() }
    }

    @Test
    fun refreshDevices() {
        val devices = listOf(SpotifyPlaybackDevice(id = "1"), SpotifyPlaybackDevice(id = "2"))
        coEvery { Spotify.Player.getAvailableDevices() } delayed 1000 returns devices

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    assertDevices(refreshing = false, devices = null)

                    PlayerRepository.refreshDevices()

                    assertDevices(refreshing = false, devices = null)

                    runCurrent()

                    assertDevices(refreshing = true, devices = null)

                    advanceUntilIdle()

                    assertDevices(refreshing = false, devices = devices)
                    assertThat(errors).isEmpty()
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getAvailableDevices() }
    }

    @Test
    fun refreshDevicesCancelled() {
        coEvery { Spotify.Player.getAvailableDevices() } delayed 1000 throws CancellationException()

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    PlayerRepository.refreshDevices()

                    advanceUntilIdle()

                    assertDevices(refreshing = false, devices = null)
                    assertThat(errors).isEmpty()
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getAvailableDevices() }
    }

    @Test
    fun refreshDevicesError() {
        val error = IOException()
        coEvery { Spotify.Player.getAvailableDevices() } delayed 1000 throws error

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    PlayerRepository.refreshDevices()

                    advanceUntilIdle()

                    assertDevices(refreshing = false, devices = null)
                    assertThat(errors).containsExactly(error)
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getAvailableDevices() }
    }

    @Test
    fun refreshDevicesParallel() {
        val devices = listOf(SpotifyPlaybackDevice(id = "1"), SpotifyPlaybackDevice(id = "2"))
        coEvery { Spotify.Player.getAvailableDevices() } delayed 1000 returns devices

        runTest {
            Repository.withRepositoryScope(scope = this) {
                collectingToList(PlayerRepository.errors) { errors ->
                    launch {
                        PlayerRepository.refreshDevices()
                    }

                    launch {
                        PlayerRepository.refreshDevices()
                    }

                    advanceUntilIdle()

                    assertDevices(refreshing = false, devices = devices)
                    assertThat(errors).isEmpty()
                }
            }
        }

        coVerify(exactly = 1) { Spotify.Player.getAvailableDevices() }
    }

    private fun assertPlayback(refreshing: Boolean, playback: SpotifyPlayback?) {
        assertThat(PlayerRepository.refreshingPlayback.value).isEqualTo(refreshing)
        assertThat(PlayerRepository.currentTrack.value).isEqualTo(playback?.item)
        assertThat(PlayerRepository.currentDevice.value).isEqualTo(playback?.device)

        if (playback?.progressMs != null) {
            val trackPosition = requireNotNull(PlayerRepository.trackPosition.value) as TrackPosition.Fetched
            assertThat(trackPosition.fetchedPositionMs.toLong()).isEqualTo(playback.progressMs)
        } else {
            assertThat(PlayerRepository.trackPosition.value).isNull()
        }

        assertThat(PlayerRepository.playing.value?.value).isEqualTo(playback?.isPlaying)
        assertThat(PlayerRepository.shuffling.value?.value).isEqualTo(playback?.shuffleState)
        assertThat(PlayerRepository.repeatMode.value?.value).isEqualTo(playback?.repeatState)
        assertThat(PlayerRepository.playbackContextUri.value).isEqualTo(playback?.context?.uri)
        assertThat(PlayerRepository.currentlyPlayingType.value).isEqualTo(playback?.currentlyPlayingType)

        assertThat(PlayerRepository.skipping.value).isEqualTo(
            when {
                playback?.actions?.skippingNext == true -> SkippingState.SKIPPING_TO_NEXT
                playback?.actions?.skippingPrev == true -> SkippingState.SKIPPING_TO_PREVIOUS
                else -> SkippingState.NOT_SKIPPING
            },
        )
    }

    private fun assertTrackPlayback(refreshing: Boolean, playback: SpotifyTrackPlayback?) {
        assertThat(PlayerRepository.refreshingTrack.value).isEqualTo(refreshing)
        assertThat(PlayerRepository.currentTrack.value).isEqualTo(playback?.item)

        if (playback?.progressMs != null) {
            val trackPosition = requireNotNull(PlayerRepository.trackPosition.value) as TrackPosition.Fetched
            assertThat(trackPosition.fetchedPositionMs.toLong()).isEqualTo(playback.progressMs)
        } else {
            assertThat(PlayerRepository.trackPosition.value).isNull()
        }

        assertThat(PlayerRepository.playing.value?.value).isEqualTo(playback?.isPlaying)
        assertThat(PlayerRepository.playbackContextUri.value).isEqualTo(playback?.context?.uri)
        assertThat(PlayerRepository.currentlyPlayingType.value).isEqualTo(playback?.currentlyPlayingType)
    }

    private fun assertDevices(refreshing: Boolean, devices: List<SpotifyPlaybackDevice>?) {
        assertThat(PlayerRepository.refreshingDevices.value).isEqualTo(refreshing)
        assertThat(PlayerRepository.availableDevices.value).isEqualTo(devices)
    }
}
