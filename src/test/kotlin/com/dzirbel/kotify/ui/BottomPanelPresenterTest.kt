package com.dzirbel.kotify.ui

import androidx.compose.runtime.MutableState
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.PlaybackDevice
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyAll
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// TODO finish testing
internal class BottomPanelPresenterTest {
    private val currentDeviceState: MutableState<PlaybackDevice?> = mockk {
        every { value = any() } just Runs
    }

    @BeforeEach
    fun setup() {
        unmockkAll()
        mockkObject(Spotify.Player, SpotifyCache, Player)
        every { Player.currentDevice } returns currentDeviceState

        coEvery { Spotify.Player.getAvailableDevices() } returns emptyList()
        coEvery { Spotify.Player.getCurrentPlayback() } returns null
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } returns null
    }

    @AfterEach
    fun finish() {
        confirmVerified(Spotify.Player)
        confirmVerified(SpotifyCache)
        confirmVerified(Player)
        confirmVerified(currentDeviceState)
    }

    @Test
    fun initialState() {
        testPresenter(
            createPresenter = ::BottomPanelPresenter,
            beforeOpen = { presenter ->
                assertThat(presenter.testState.stateOrThrow).isEqualTo(loadingState)
            }
        ) {
            verifyOpenCalls()
        }
    }

    @Test
    fun loadDevices() {
        testPresenter(::BottomPanelPresenter) { presenter ->
            verifyOpenCalls()

            val device1: PlaybackDevice = mockk()
            val device2: PlaybackDevice = mockk()
            coEvery { Spotify.Player.getAvailableDevices() } returns listOf(device1, device2)

            presenter.emit(BottomPanelPresenter.Event.LoadDevices())

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.copy(devices = listOf(device1, device2)))

            coVerify {
                Spotify.Player.getAvailableDevices()
                Player.currentDevice
                currentDeviceState.value = device1
            }
        }
    }

    @Test
    fun loadDevicesUntilVolumeChange() {
        val device: PlaybackDevice = mockk {
            every { id } returns "device_id"
            every { volumePercent } returnsMany listOf(20, 20, 20, 30)
        }
        coEvery { Spotify.Player.getAvailableDevices() } returns listOf(device)
        testPresenter(::BottomPanelPresenter) { presenter ->
            verifyOpenCalls(device = device)

            presenter.emit(
                BottomPanelPresenter.Event.LoadDevices(
                    untilVolumeChange = true,
                    untilVolumeChangeDeviceId = "device_id"
                )
            )

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.copy(devices = listOf(device)))

            coVerify(exactly = 3) {
                Spotify.Player.getAvailableDevices()
            }

            coVerify {
                Player.currentDevice
                currentDeviceState.value = device
            }
        }
    }

    private fun verifyOpenCalls(device: PlaybackDevice? = null) {
        coVerifyAll {
            Spotify.Player.getAvailableDevices()
            Spotify.Player.getCurrentPlayback()
            Spotify.Player.getCurrentlyPlayingTrack()

            Player.playEvents
            Player.currentDevice
            currentDeviceState.value = device
        }
    }

    companion object {
        private val loadingState = BottomPanelPresenter.State()
        private val loadedState = BottomPanelPresenter.State().copy(
            loadingPlayback = false,
            loadingTrackPlayback = false,
            loadingDevices = false
        )
    }
}
