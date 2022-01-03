package com.dzirbel.kotify.ui

import androidx.compose.runtime.MutableState
import com.dzirbel.kotify.cache.LibraryCache
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.db.model.AlbumRepository
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackContext
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

// TODO finish testing
internal class BottomPanelPresenterTest {
    private val currentDeviceState: MutableState<SpotifyPlaybackDevice?> = mockk {
        every { value = any() } just Runs
    }
    private val currentTrackState: MutableState<FullSpotifyTrack?> = mockk {
        every { value = any() } just Runs
    }
    private val isPlayingState: MutableState<Boolean> = mockk {
        every { value = any() } just Runs
    }
    private val playbackContextState: MutableState<SpotifyPlaybackContext?> = mockk {
        every { value = any() } just Runs
    }

    @BeforeEach
    fun setup() {
        unmockkAll()
        mockkObject(Spotify.Player, SpotifyCache, LibraryCache, Player, AlbumRepository)

        every { Player.currentDevice } returns currentDeviceState
        every { Player.currentTrack } returns currentTrackState
        every { Player.isPlaying } returns isPlayingState
        every { Player.playbackContext } returns playbackContextState

        coEvery { Spotify.Player.getAvailableDevices() } returns emptyList()
        coEvery { Spotify.Player.getCurrentPlayback() } returns null
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } returns null

        coEvery { AlbumRepository.isSaved(any()) } returns false
        every { LibraryCache.savedArtists } returns emptySet()
        every { LibraryCache.savedTracks } returns emptySet()
    }

    @AfterEach
    fun finish() {
        confirmVerified(Spotify.Player, SpotifyCache, LibraryCache, Player)
        confirmVerified(currentDeviceState, currentTrackState, isPlayingState, playbackContextState)
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

            val device1: SpotifyPlaybackDevice = mockk()
            val device2: SpotifyPlaybackDevice = mockk()
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
        val device: SpotifyPlaybackDevice = mockk {
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

    @ParameterizedTest
    @MethodSource("playback")
    fun loadPlayback(playback: SpotifyPlayback?) {
        testPresenter(::BottomPanelPresenter) { presenter ->
            verifyOpenCalls()

            coEvery { Spotify.Player.getCurrentPlayback() } returns playback

            presenter.emit(BottomPanelPresenter.Event.LoadPlayback())

            assertThat(presenter.testState.stateOrThrow).isEqualTo(
                loadedState.copy(
                    loadingPlayback = false,
                    playbackProgressMs = playback?.progressMs,
                    playbackIsPlaying = playback?.isPlaying,
                    playbackShuffleState = playback?.shuffleState,
                    playbackRepeatState = playback?.repeatState,
                    playbackCurrentDevice = playback?.device,
                    playbackTrack = playback?.item,
                    trackIsSaved = playback?.item?.let { false },
                    albumIsSaved = playback?.item?.let { false },
                    artistsAreSaved = playback?.item?.let { emptyMap() },
                )
            )

            coVerify {
                Spotify.Player.getCurrentPlayback()
                if (playback != null) {
                    Player.isPlaying
                    isPlayingState.value = playback.isPlaying

                    Player.playbackContext
                    playbackContextState.value = playback.context

                    playback.item?.let { track ->
                        SpotifyCache.put(track)

                        Player.currentTrack
                        currentTrackState.value = track

                        AlbumRepository.isSaved(playback.item!!.album.id!!)
                        LibraryCache.savedArtists
                        LibraryCache.savedTracks
                    }
                }
            }
        }
    }

    private fun verifyOpenCalls(device: SpotifyPlaybackDevice? = null) {
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
        private val loadedState = BottomPanelPresenter.State(
            loadingPlayback = false,
            loadingTrackPlayback = false,
            loadingDevices = false,
            devices = emptyList()
        )

        @JvmStatic
        @Suppress("unused")
        fun playback(): List<SpotifyPlayback?> {
            return listOf(
                null,
                SpotifyPlayback(
                    timestamp = 123,
                    device = mockk(),
                    progressMs = 456,
                    isPlaying = true,
                    currentlyPlayingType = "type",
                    item = null,
                    shuffleState = true,
                    repeatState = "repeat",
                    context = null
                ),
                SpotifyPlayback(
                    timestamp = 0,
                    device = mockk(),
                    progressMs = 0,
                    isPlaying = false,
                    currentlyPlayingType = "type",
                    item = mockk(relaxed = true),
                    shuffleState = false,
                    repeatState = "repeat",
                    context = mockk()
                ),
            )
        }
    }
}
