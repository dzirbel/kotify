package com.dzirbel.kotify.ui

import androidx.compose.runtime.MutableState
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.FixtureModels
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackContext
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyTrackPlayback
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
        coEvery { Spotify.Player.skipToNext(deviceId = any()) } just Runs
        coEvery { Spotify.Player.skipToPrevious(deviceId = any()) } just Runs
        coEvery { Spotify.Player.toggleShuffle(state = any(), deviceId = any()) } just Runs
        coEvery { Spotify.Player.setRepeatMode(state = any(), deviceId = any()) } just Runs
        coEvery { Spotify.Player.setVolume(volumePercent = any(), deviceId = any()) } just Runs
        coEvery { Spotify.Player.seekToPosition(positionMs = any(), deviceId = any()) } just Runs
        coEvery { Spotify.Player.transferPlayback(deviceIds = any()) } just Runs

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
        val device1: SpotifyPlaybackDevice = device(id = "device 1")
        val device2: SpotifyPlaybackDevice = device(id = "device 2")
        coEvery { Spotify.Player.getAvailableDevices() } returnsMany listOf(emptyList(), listOf(device1, device2))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls()

            presenter.emit(PlayerPanelPresenter.Event.LoadDevices())
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.copy(devices = listOf(device1, device2)))

            coVerify {
                Spotify.Player.getAvailableDevices()
                Player.currentPlaybackDeviceId
                currentPlaybackDeviceIdState.value = device1.id
            }
        }
    }

    @Test
    fun loadDevicesUntilVolumeChange() {
        val device1 = device(volumePercent = 20)
        val device2 = device(volumePercent = 20)
        val device3 = device(volumePercent = 20)
        val device4 = device(volumePercent = 50)
        coEvery { Spotify.Player.getAvailableDevices() } returnsMany
            listOf(listOf(device1), listOf(device2), listOf(device3), listOf(device4))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(deviceId = deviceId)

            presenter.emit(
                PlayerPanelPresenter.Event.LoadDevices(
                    untilVolumeChange = true,
                    untilVolumeChangeDeviceId = deviceId,
                ),
            )
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.copy(devices = listOf(device4)))

            coVerify(exactly = 4) {
                Spotify.Player.getAvailableDevices()
            }

            coVerify {
                Player.currentPlaybackDeviceId
                currentPlaybackDeviceIdState.value = deviceId
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

    @Test
    fun skipNext() {
        val playbackA = trackPlayback(track = trackA)
        val playbackB = trackPlayback(track = trackB)
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } returnsMany listOf(playbackA, playbackA, playbackB)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(trackPlayback = playbackA)

            presenter.emit(PlayerPanelPresenter.Event.SkipNext)
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.withTrackPlayback(playbackB))

            coVerify { Spotify.Player.skipToNext() }
            verifyLoadTrackPlaybackCalls(trackPlayback(track = trackA))
            verifyLoadTrackPlaybackCalls(trackPlayback(track = trackB))
        }
    }

    @Test
    fun skipPrevious() {
        val playbackA = trackPlayback(track = trackA)
        val playbackB = trackPlayback(track = trackB)
        coEvery { Spotify.Player.getCurrentlyPlayingTrack() } returnsMany listOf(playbackA, playbackA, playbackB)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(trackPlayback = playbackA)

            presenter.emit(PlayerPanelPresenter.Event.SkipPrevious)
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.withTrackPlayback(playbackB))

            coVerify { Spotify.Player.skipToPrevious() }
            verifyLoadTrackPlaybackCalls(trackPlayback(track = trackA))
            verifyLoadTrackPlaybackCalls(trackPlayback(track = trackB))
        }
    }

    @Test
    fun toggleShuffle() {
        val playbackShuffle = playback(shuffleState = true)
        val playbackNoShuffle = playback(shuffleState = false)
        coEvery { Spotify.Player.getCurrentPlayback() } returnsMany
            listOf(playbackNoShuffle, playbackNoShuffle, playbackShuffle)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(playback = playbackNoShuffle)

            presenter.emit(PlayerPanelPresenter.Event.ToggleShuffle(shuffle = true))
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.withPlayback(playbackShuffle))

            coVerify { Spotify.Player.toggleShuffle(state = true, deviceId = deviceId) }
            verifyLoadPlaybackCalls(playbackNoShuffle)
            verifyLoadPlaybackCalls(playbackShuffle)
        }
    }

    @Test
    fun setRepeat() {
        val playbackRepeatA = playback(repeatState = "A")
        val playbackRepeatB = playback(repeatState = "B")
        coEvery { Spotify.Player.getCurrentPlayback() } returnsMany
            listOf(playbackRepeatA, playbackRepeatA, playbackRepeatB)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(playback = playbackRepeatA)

            presenter.emit(PlayerPanelPresenter.Event.SetRepeat(repeatState = "B"))
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.withPlayback(playbackRepeatB))

            coVerify { Spotify.Player.setRepeatMode(state = "B", deviceId = deviceId) }
            verifyLoadPlaybackCalls(playbackRepeatA)
            verifyLoadPlaybackCalls(playbackRepeatB)
        }
    }

    @Test
    fun setVolume() {
        val oldVolume = 40
        val newVolume = 60

        val device1 = device(volumePercent = oldVolume)
        val device2 = device(volumePercent = newVolume)
        coEvery { Spotify.Player.getAvailableDevices() } returnsMany
            listOf(listOf(device1), listOf(device1), listOf(device2))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(deviceId = deviceId)

            presenter.emit(PlayerPanelPresenter.Event.SetVolume(volume = newVolume))
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(
                loadedState.copy(devices = listOf(device2), savedVolume = 60),
            )

            coVerify { Spotify.Player.setVolume(volumePercent = newVolume, deviceId = deviceId) }
            coVerify(exactly = 3) { Spotify.Player.getAvailableDevices() }

            coVerify {
                Player.currentPlaybackDeviceId
                currentPlaybackDeviceIdState.value = deviceId
            }
        }
    }

    @Test
    fun toggleMuteVolume() {
        val originalVolume = 40

        val deviceUnmuted = device(volumePercent = originalVolume)
        val deviceMuted = device(volumePercent = 0)
        coEvery { Spotify.Player.getAvailableDevices() } returnsMany listOf(
            listOf(deviceUnmuted),
            listOf(deviceUnmuted),
            listOf(deviceMuted),
            listOf(deviceMuted),
            listOf(deviceUnmuted),
        )

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(deviceId = deviceId)

            presenter.emit(PlayerPanelPresenter.Event.ToggleMuteVolume(mute = true, previousVolume = originalVolume))
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(
                loadedState.copy(devices = listOf(deviceMuted), savedVolume = originalVolume),
            )

            presenter.emit(PlayerPanelPresenter.Event.ToggleMuteVolume(mute = false, previousVolume = originalVolume))
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(
                loadedState.copy(devices = listOf(deviceUnmuted), savedVolume = originalVolume),
            )

            coVerify { Spotify.Player.setVolume(volumePercent = 0, deviceId = deviceId) }
            coVerify { Spotify.Player.setVolume(volumePercent = originalVolume, deviceId = deviceId) }
            coVerify(exactly = 5) { Spotify.Player.getAvailableDevices() }

            coVerify {
                Player.currentPlaybackDeviceId
                currentPlaybackDeviceIdState.value = deviceId
            }
        }
    }

    @Test
    fun seekTo() {
        val playback1 = playback(progressMs = 200)
        val playback2 = playback(progressMs = 100)

        coEvery { Spotify.Player.getCurrentPlayback() } returnsMany listOf(playback1, playback2)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls(playback = playback1)

            presenter.emit(PlayerPanelPresenter.Event.SeekTo(positionMs = 100))
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.withPlayback(playback2))

            coVerify { Spotify.Player.seekToPosition(positionMs = 100, deviceId = deviceId) }
            verifyLoadPlaybackCalls(playback = playback2)
        }
    }

    @Test
    fun selectDevice() {
        val newDevice = device(id = "new device")

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenCalls()

            presenter.emit(PlayerPanelPresenter.Event.SelectDevice(device = newDevice))
            advanceUntilIdle()

            assertThat(presenter.testState.stateOrThrow).isEqualTo(loadedState.copy(selectedDevice = newDevice))

            coVerify { Spotify.Player.transferPlayback(deviceIds = listOf(newDevice.id)) }
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

    private fun verifyLoadTrackPlaybackCalls(playback: SpotifyTrackPlayback?) {
        coVerify {
            Spotify.Player.getCurrentlyPlayingTrack()
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

    private fun verifyOpenCalls(
        deviceId: String? = null,
        playback: SpotifyPlayback? = null,
        trackPlayback: SpotifyTrackPlayback? = null,
    ) {
        coVerify {
            Spotify.Player.getAvailableDevices()

            Player.playEvents
            Player.currentPlaybackDeviceId
            currentPlaybackDeviceIdState.value = deviceId
        }

        verifyLoadPlaybackCalls(playback)
        verifyLoadTrackPlaybackCalls(trackPlayback)
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

    private suspend fun PlayerPanelPresenter.ViewModel.withTrackPlayback(
        trackPlayback: SpotifyTrackPlayback?,
    ): PlayerPanelPresenter.ViewModel {
        return copy(
            loadingPlayback = false,
            playbackProgressMs = trackPlayback?.progressMs,
            playbackIsPlaying = trackPlayback?.isPlaying,
            playbackTrack = trackPlayback?.item,
            trackSavedState = trackPlayback?.item?.let { SavedTrackRepository.stateOf(id = it.id) },
            trackRatingState = trackPlayback?.item?.let { TrackRatingRepository.ratingState(id = it.id) },
            albumSavedState = trackPlayback?.item?.album?.id?.let { SavedAlbumRepository.stateOf(id = it) },
            artistSavedStates = trackPlayback?.item?.let { emptyMap() },
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
        val device: SpotifyPlaybackDevice = device()
        val track: FullSpotifyTrack = FixtureModels.networkFullTrack()
        val trackA: FullSpotifyTrack = FixtureModels.networkFullTrack(id = "track-a", name = "Track A")
        val trackB: FullSpotifyTrack = FixtureModels.networkFullTrack(id = "track-b", name = "Track B")

        val playbackNotPlaying = playback(isPlaying = false, track = null)
        val playbackPlaying = playback(isPlaying = true)

        fun device(id: String = deviceId, volumePercent: Int = 50): SpotifyPlaybackDevice {
            return SpotifyPlaybackDevice(
                id = id,
                isActive = true,
                isRestricted = false,
                isPrivateSession = null,
                name = "my device",
                type = "computer",
                volumePercent = volumePercent,
            )
        }

        fun playback(
            isPlaying: Boolean = true,
            device: SpotifyPlaybackDevice = this.device,
            track: FullSpotifyTrack? = this.track,
            shuffleState: Boolean = false,
            repeatState: String = "repeat",
            progressMs: Long = 456,
        ): SpotifyPlayback {
            return SpotifyPlayback(
                timestamp = 123,
                device = device,
                progressMs = progressMs,
                isPlaying = isPlaying,
                currentlyPlayingType = "type",
                item = track,
                shuffleState = shuffleState,
                repeatState = repeatState,
                context = null,
            )
        }

        fun trackPlayback(
            isPlaying: Boolean = false,
            track: FullSpotifyTrack? = this.track,
        ): SpotifyTrackPlayback {
            return SpotifyTrackPlayback(
                timestamp = 123,
                progressMs = 456,
                isPlaying = isPlaying,
                currentlyPlayingType = "type",
                item = track,
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
