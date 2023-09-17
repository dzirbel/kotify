package com.dzirbel.kotify.repository.player

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
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
class PlayerRepositoryRefreshTracksTest : BasePlayerRepositoryTest() {
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

    private fun PlayerRepository.assertDevices(refreshing: Boolean, devices: List<SpotifyPlaybackDevice>?) {
        assertThat(refreshingDevices.value).isEqualTo(refreshing)
        assertThat(availableDevices.value).isEqualTo(devices)
    }
}
