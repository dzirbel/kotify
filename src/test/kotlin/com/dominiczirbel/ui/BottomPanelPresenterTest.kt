package com.dominiczirbel.ui

import androidx.compose.runtime.MutableState
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.PlaybackDevice
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

// TODO finish testing
internal class BottomPanelPresenterTest {
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
                assertThat(presenter.testState.stateOrThrow).isEqualTo(BottomPanelPresenter.State())
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

            assertThat(presenter.testState.stateOrThrow).isEqualTo(
                BottomPanelPresenter.State().copy(
                    loadingPlayback = false,
                    loadingTrackPlayback = false,
                    loadingDevices = false,
                    devices = listOf(device1, device2)
                )
            )

            coVerify {
                Spotify.Player.getAvailableDevices()
                Player.currentDevice
                currentDeviceState.value = device1
            }
        }
    }

    private fun verifyOpenCalls() {
        coVerifyAll {
            Spotify.Player.getAvailableDevices()
            Spotify.Player.getCurrentPlayback()
            Spotify.Player.getCurrentlyPlayingTrack()

            Player.playEvents
            Player.currentDevice
            currentDeviceState.value = null
        }
    }

    companion object {
        private val currentDeviceState: MutableState<PlaybackDevice?> = mockk {
            every { value = any() } just Runs
        }

        @BeforeAll
        @JvmStatic
        @Suppress("unused")
        fun setup() {
            mockkObject(Spotify.Player, SpotifyCache, Player)
            every { Player.currentDevice } returns currentDeviceState

            coEvery { Spotify.Player.getAvailableDevices() } returns emptyList()
            coEvery { Spotify.Player.getCurrentPlayback() } returns null
            coEvery { Spotify.Player.getCurrentlyPlayingTrack() } returns null
        }
    }
}
