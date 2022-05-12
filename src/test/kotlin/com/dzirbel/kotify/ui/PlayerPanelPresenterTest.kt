package com.dzirbel.kotify.ui

import androidx.compose.runtime.MutableState
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackContext
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.player.PlayerPanelPresenter
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

// TODO finish testing
internal class PlayerPanelPresenterTest {
    private val currentPlaybackDeviceIdState: MutableState<String?> = mockk {
        every { value = any() } just Runs
    }
    private val currentTrackIdState: MutableState<String?> = mockk {
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
        mockkObject(Spotify.Player, Spotify.Library, Player)

        every { Player.currentPlaybackDeviceId } returns currentPlaybackDeviceIdState
        every { Player.currentTrackId } returns currentTrackIdState
        every { Player.isPlaying } returns isPlayingState
        every { Player.playbackContext } returns playbackContextState

        coEvery { Spotify.Player.getAvailableDevices() } returns emptyList()
        coEvery { Spotify.Player.getCurrentPlayback() } returns null
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } returns null
        coEvery { Spotify.Player.startPlayback(deviceId = any()) } just Runs
        coEvery { Spotify.Player.pausePlayback(deviceId = any()) } just Runs

        coEvery { Spotify.Library.checkTracks(any()) } answers { firstArg<List<String>>().map { false } }
        coEvery { Spotify.Library.checkAlbums(any()) } answers { firstArg<List<String>>().map { false } }
    }

    @AfterEach
    fun finish() {
        confirmVerified(Spotify.Player, Player)
        confirmVerified(currentPlaybackDeviceIdState, currentTrackIdState, isPlayingState, playbackContextState)
        unmockkAll()
    }

    @Test
    fun initialState() {
        testPresenter(
            createPresenter = ::PlayerPanelPresenter,
            beforeOpen = { presenter ->
                assertThat(presenter.testState.stateOrThrow).isEqualTo(loadingState)
            },
        ) {
            verifyOpenCalls()
        }
    }

    @Test
    fun loadDevices() {
        val device1: SpotifyPlaybackDevice = mockk {
            every { id } returns "device 1"
        }
        val device2: SpotifyPlaybackDevice = mockk {
            every { id } returns "device 2"
        }
        coEvery { Spotify.Player.getAvailableDevices() } returnsMany listOf(emptyList(), listOf(device1, device2))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls()

            presenter.emit(PlayerPanelPresenter.Event.LoadDevices())
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.copy(devices = listOf(device1, device2)))

            coVerify {
                Spotify.Player.getAvailableDevices()
                Player.currentPlaybackDeviceId
                currentPlaybackDeviceIdState.value = "device 1"
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

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(deviceId = "device_id")

            presenter.emit(
                PlayerPanelPresenter.Event.LoadDevices(
                    untilVolumeChange = true,
                    untilVolumeChangeDeviceId = "device_id",
                ),
            )
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.copy(devices = listOf(device)))

            coVerify(exactly = 3) {
                Spotify.Player.getAvailableDevices()
            }

            coVerify {
                Player.currentPlaybackDeviceId
                currentPlaybackDeviceIdState.value = "device_id"
            }
        }
    }

    @ParameterizedTest
    @MethodSource("playbacks")
    fun loadPlayback(playback: SpotifyPlayback?) {
        coEvery { Spotify.Player.getCurrentPlayback() } returns playback

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls()

            presenter.emit(PlayerPanelPresenter.Event.LoadPlayback())
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.withPlayback(playback))

            verifyLoadPlaybackCalls(playback)
        }
    }

    @Test
    fun play() {
        coEvery { Spotify.Player.getCurrentPlayback() } returnsMany
            listOf(playbackNotPlaying, playbackNotPlaying, playbackPlaying)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(playback = playbackNotPlaying)

            presenter.emit(PlayerPanelPresenter.Event.Play)
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.withPlayback(playbackPlaying))

            coVerify { Spotify.Player.startPlayback(deviceId = deviceId) }
            verifyLoadPlaybackCalls(playbackNotPlaying)
            verifyLoadPlaybackCalls(playbackPlaying)
        }
    }

    @Test
    fun pause() {
        coEvery { Spotify.Player.getCurrentPlayback() } returnsMany
            listOf(playbackPlaying, playbackPlaying, playbackNotPlaying)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(playback = playbackPlaying)

            presenter.emit(PlayerPanelPresenter.Event.Pause)
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.withPlayback(playbackNotPlaying))

            coVerify { Spotify.Player.pausePlayback(deviceId = deviceId) }
            verifyLoadPlaybackCalls(playbackPlaying)
            verifyLoadPlaybackCalls(playbackNotPlaying)
        }
    }

    private fun verifyLoadPlaybackCalls(playback: SpotifyPlayback?) {
        coVerify {
            Spotify.Player.getCurrentPlayback()
            if (playback != null) {
                Player.isPlaying
                isPlayingState.value = playback.isPlaying

                Player.playbackContext
                playbackContextState.value = playback.context

                playback.item?.let { track ->
                    Player.currentTrackId
                    currentTrackIdState.value = track.id
                }
            }
        }
    }

    private fun verifyOpenCalls(deviceId: String? = null, playback: SpotifyPlayback? = null) {
        coVerify {
            Spotify.Player.getAvailableDevices()
            Spotify.Player.getCurrentlyPlayingTrack()

            Player.playEvents
            Player.currentPlaybackDeviceId
            currentPlaybackDeviceIdState.value = deviceId
        }

        verifyLoadPlaybackCalls(playback)
    }

    private suspend fun PlayerPanelPresenter.ViewModel.withPlayback(
        playback: SpotifyPlayback?,
    ): PlayerPanelPresenter.ViewModel {
        return copy(
            loadingPlayback = false,
            playbackProgressMs = playback?.progressMs,
            playbackIsPlaying = playback?.isPlaying,
            playbackShuffleState = playback?.shuffleState,
            playbackRepeatState = playback?.repeatState,
            playbackCurrentDevice = playback?.device,
            playbackTrack = playback?.item,
            trackSavedState = playback?.item?.let { SavedTrackRepository.stateOf(id = it.id) },
            trackRatingState = playback?.item?.let { TrackRatingRepository.ratingState(id = it.id) },
            albumSavedState = playback?.item?.album?.id?.let { SavedAlbumRepository.stateOf(id = it) },
            artistSavedStates = playback?.item?.let { emptyMap() },
        )
    }

    companion object {
        private val loadingState = PlayerPanelPresenter.ViewModel()
        private val loadedState = PlayerPanelPresenter.ViewModel(
            loadingPlayback = false,
            loadingTrackPlayback = false,
            loadingDevices = false,
            devices = emptyList(),
        )

        const val deviceId = "device"
        val device: SpotifyPlaybackDevice = mockk {
            every { id } returns deviceId
        }
        val track: FullSpotifyTrack = mockk(relaxed = true)

        val playbackNotPlaying = playback(isPlaying = false, track = null)
        val playbackPlaying = playback(isPlaying = true)

        fun playback(
            isPlaying: Boolean = true,
            device: SpotifyPlaybackDevice = this.device,
            track: FullSpotifyTrack? = this.track,
        ): SpotifyPlayback {
            return SpotifyPlayback(
                timestamp = 123,
                device = device,
                progressMs = 456,
                isPlaying = isPlaying,
                currentlyPlayingType = "type",
                item = track,
                shuffleState = true,
                repeatState = "repeat",
                context = null,
            )
        }

        @JvmStatic
        @Suppress("unused")
        fun playbacks(): List<SpotifyPlayback?> {
            return listOf(
                null,
                playback(isPlaying = false),
                playback(isPlaying = false, track = null),
                playback(isPlaying = true),
            )
        }
    }
}
