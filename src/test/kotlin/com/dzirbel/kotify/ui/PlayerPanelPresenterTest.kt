package com.dzirbel.kotify.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.dzirbel.kotify.FixtureModels
import com.dzirbel.kotify.assertStateEquals
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.emitAndIdle
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.TestSpotifyInterceptor
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyTrackPlayback
import com.dzirbel.kotify.testPresenter
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.player.PlayerPanelPresenter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class PlayerPanelPresenterTest {
    @BeforeEach
    fun setup() {
        TestSpotifyInterceptor.intercept(Spotify.Player.GET_CURRENT_PLAYBACK_PATH, null)
        TestSpotifyInterceptor.intercept(Spotify.Player.GET_CURRENT_PLAYING_TRACK_PATH, null)
        TestSpotifyInterceptor.intercept(
            Spotify.Player.GET_AVAILABLE_DEVICES_PATH,
            Spotify.Player.AvailableDevicesResponse(devices = emptyList()),
        )
    }

    @AfterEach
    fun finish() {
        TestSpotifyInterceptor.verifyAllIntercepted()
        Player.resetState()
    }

    @Test
    fun initialState() {
        testPresenter(
            createPresenter = ::PlayerPanelPresenter,
            beforeOpen = { presenter ->
                assertThat(presenter.testState.stateOrThrow).isEqualTo(loadingState)
            },
        ) { presenter ->
            verifyOpenState(playback = null, trackPlayback = null, deviceId = null)
            presenter.assertStateEquals(loadedState)
        }
    }

    @Test
    fun loadDevices() {
        val device1: SpotifyPlaybackDevice = device(id = "device 1")
        val device2: SpotifyPlaybackDevice = device(id = "device 2")
        TestSpotifyInterceptor.intercept(
            Spotify.Player.GET_AVAILABLE_DEVICES_PATH,
            Spotify.Player.AvailableDevicesResponse(devices = emptyList()),
            Spotify.Player.AvailableDevicesResponse(devices = listOf(device1, device2)),
        )

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = null, trackPlayback = null, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.LoadDevices())

            presenter.assertStateEquals(loadedState.copy(devices = listOf(device1, device2)))
            assertThat(Player.currentPlaybackDeviceId.value).isEqualTo(device1.id)
        }
    }

    @Test
    fun loadDevicesUntilVolumeChange() {
        val device1 = device(volumePercent = 20)
        val device2 = device(volumePercent = 20)
        val device3 = device(volumePercent = 20)
        val device4 = device(volumePercent = 50)
        TestSpotifyInterceptor.intercept(
            Spotify.Player.GET_AVAILABLE_DEVICES_PATH,
            Spotify.Player.AvailableDevicesResponse(devices = listOf(device1)),
            Spotify.Player.AvailableDevicesResponse(devices = listOf(device2)),
            Spotify.Player.AvailableDevicesResponse(devices = listOf(device3)),
            Spotify.Player.AvailableDevicesResponse(devices = listOf(device4)),
        )

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = null, trackPlayback = null, deviceId = deviceId)

            presenter.emitAndIdle(
                PlayerPanelPresenter.Event.LoadDevices(untilVolumeChange = true, untilVolumeChangeDeviceId = deviceId),
            )

            presenter.assertStateEquals(loadedState.copy(devices = listOf(device4)))
            assertThat(Player.currentPlaybackDeviceId.value).isEqualTo(deviceId)
        }
    }

    @ParameterizedTest
    @MethodSource("playbacks")
    fun loadPlayback(playback: SpotifyPlayback?) {
        TestSpotifyInterceptor.intercept(Spotify.Player.GET_CURRENT_PLAYBACK_PATH, playback, playback)
        if (playback?.item != null) {
            TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_TRACKS_PATH, listOf(false))
            if (playback.item?.album != null) {
                TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_ALBUMS_PATH, listOf(false))
            }
        }

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = playback, trackPlayback = null, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.LoadPlayback())

            presenter.assertStateEquals(loadedState.withPlayback(playback))
            Player.verifyState(playback = playback, trackPlayback = null, deviceId = null)
        }
    }

    @Test
    fun play() {
        TestSpotifyInterceptor.intercept(
            Spotify.Player.GET_CURRENT_PLAYBACK_PATH,
            playbackNotPlaying,
            playbackNotPlaying,
            playbackPlaying,
        )
        TestSpotifyInterceptor.intercept(Spotify.Player.START_PLAYBACK_PATH, method = "PUT", Unit)
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_TRACKS_PATH, listOf(false))
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_ALBUMS_PATH, listOf(false))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = playbackNotPlaying, trackPlayback = null, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.Play)

            presenter.assertStateEquals(loadedState.withPlayback(playbackPlaying))
            Player.verifyState(playback = playbackPlaying, trackPlayback = null, deviceId = null)
        }
    }

    @Test
    fun pause() {
        TestSpotifyInterceptor.intercept(
            Spotify.Player.GET_CURRENT_PLAYBACK_PATH,
            playbackPlaying,
            playbackPlaying,
            playbackNotPlaying,
        )
        TestSpotifyInterceptor.intercept(Spotify.Player.PAUSE_PLAYBACK_PATH, method = "PUT", Unit)
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_TRACKS_PATH, listOf(false))
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_ALBUMS_PATH, listOf(false))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = playbackPlaying, trackPlayback = null, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.Pause)

            presenter.assertStateEquals(loadedState.withPlayback(playbackNotPlaying))
            Player.verifyState(playback = playbackNotPlaying, trackPlayback = null, deviceId = null)
        }
    }

    @Test
    fun skipNext() {
        val playbackA = trackPlayback(track = trackA)
        val playbackB = trackPlayback(track = trackB)
        TestSpotifyInterceptor.intercept(Spotify.Player.GET_CURRENT_PLAYING_TRACK_PATH, playbackA, playbackA, playbackB)
        TestSpotifyInterceptor.intercept(Spotify.Player.SKIP_TO_NEXT_PATH, method = "POST", Unit)
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_TRACKS_PATH, listOf(false), listOf(false))
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_ALBUMS_PATH, listOf(false))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = null, trackPlayback = playbackA, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.SkipNext)

            presenter.assertStateEquals(loadedState.withTrackPlayback(playbackB))
            Player.verifyState(playback = null, trackPlayback = playbackB, deviceId = null)
        }
    }

    @Test
    fun skipPrevious() {
        val playbackA = trackPlayback(track = trackA)
        val playbackB = trackPlayback(track = trackB)
        TestSpotifyInterceptor.intercept(Spotify.Player.GET_CURRENT_PLAYING_TRACK_PATH, playbackA, playbackA, playbackB)
        TestSpotifyInterceptor.intercept(Spotify.Player.SKIP_TO_PREVIOUS_PATH, method = "POST", Unit)
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_TRACKS_PATH, listOf(false), listOf(false))
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_ALBUMS_PATH, listOf(false))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = null, trackPlayback = playbackA, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.SkipPrevious)

            presenter.assertStateEquals(loadedState.withTrackPlayback(playbackB))
            Player.verifyState(playback = null, trackPlayback = playbackB, deviceId = null)
        }
    }

    @Test
    fun toggleShuffle() {
        val playbackShuffle = playback(shuffleState = true)
        val playbackNoShuffle = playback(shuffleState = false)
        TestSpotifyInterceptor.intercept(
            Spotify.Player.GET_CURRENT_PLAYBACK_PATH,
            playbackNoShuffle,
            playbackNoShuffle,
            playbackShuffle,
        )
        TestSpotifyInterceptor.intercept(Spotify.Player.TOGGLE_SHUFFLE_PATH, method = "PUT", Unit)
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_TRACKS_PATH, listOf(false))
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_ALBUMS_PATH, listOf(false))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = playbackNoShuffle, trackPlayback = null, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.ToggleShuffle(shuffle = true))

            presenter.assertStateEquals(loadedState.withPlayback(playbackShuffle))
            Player.verifyState(playback = playbackShuffle, trackPlayback = null, deviceId = null)
        }
    }

    @Test
    fun setRepeat() {
        val playbackRepeatA = playback(repeatState = "A")
        val playbackRepeatB = playback(repeatState = "B")
        TestSpotifyInterceptor.intercept(
            Spotify.Player.GET_CURRENT_PLAYBACK_PATH,
            playbackRepeatA,
            playbackRepeatA,
            playbackRepeatB,
        )
        TestSpotifyInterceptor.intercept(Spotify.Player.SET_REPEAT_MODE_PATH, method = "PUT", Unit)
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_TRACKS_PATH, listOf(false))
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_ALBUMS_PATH, listOf(false))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = playbackRepeatA, trackPlayback = null, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.SetRepeat(repeatState = "B"))

            presenter.assertStateEquals(loadedState.withPlayback(playbackRepeatB))
            Player.verifyState(playback = playbackRepeatB, trackPlayback = null, deviceId = null)
        }
    }

    @Test
    fun setVolume() {
        val oldVolume = 40
        val newVolume = 60

        val device1 = device(volumePercent = oldVolume)
        val device2 = device(volumePercent = newVolume)
        TestSpotifyInterceptor.intercept(
            Spotify.Player.GET_AVAILABLE_DEVICES_PATH,
            Spotify.Player.AvailableDevicesResponse(listOf(device1)),
            Spotify.Player.AvailableDevicesResponse(listOf(device1)),
            Spotify.Player.AvailableDevicesResponse(listOf(device2)),
        )
        TestSpotifyInterceptor.intercept(Spotify.Player.SET_VOLUME_PATH, method = "PUT", Unit)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = null, trackPlayback = null, deviceId = deviceId)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.SetVolume(volume = newVolume))

            presenter.assertStateEquals(loadedState.copy(devices = listOf(device2), savedVolume = 60))
            assertThat(Player.currentPlaybackDeviceId.value).isEqualTo(deviceId)
        }
    }

    @Test
    fun toggleMuteVolume() {
        val originalVolume = 40

        val deviceUnmuted = device(volumePercent = originalVolume)
        val deviceMuted = device(volumePercent = 0)
        TestSpotifyInterceptor.intercept(
            Spotify.Player.GET_AVAILABLE_DEVICES_PATH,
            Spotify.Player.AvailableDevicesResponse(listOf(deviceUnmuted)),
            Spotify.Player.AvailableDevicesResponse(listOf(deviceUnmuted)),
            Spotify.Player.AvailableDevicesResponse(listOf(deviceMuted)),
            Spotify.Player.AvailableDevicesResponse(listOf(deviceMuted)),
            Spotify.Player.AvailableDevicesResponse(listOf(deviceUnmuted)),
        )
        TestSpotifyInterceptor.intercept(Spotify.Player.SET_VOLUME_PATH, method = "PUT", Unit, Unit)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = null, trackPlayback = null, deviceId = deviceId)

            presenter.emitAndIdle(
                PlayerPanelPresenter.Event.ToggleMuteVolume(mute = true, previousVolume = originalVolume),
            )

            presenter.assertStateEquals(loadedState.copy(devices = listOf(deviceMuted), savedVolume = originalVolume))

            presenter.emitAndIdle(
                PlayerPanelPresenter.Event.ToggleMuteVolume(mute = false, previousVolume = originalVolume),
            )

            presenter.assertStateEquals(loadedState.copy(devices = listOf(deviceUnmuted), savedVolume = originalVolume))
            assertThat(Player.currentPlaybackDeviceId.value).isEqualTo(deviceId)
        }
    }

    @Test
    fun seekTo() {
        val playback1 = playback(progressMs = 200)
        val playback2 = playback(progressMs = 100)
        TestSpotifyInterceptor.intercept(Spotify.Player.GET_CURRENT_PLAYBACK_PATH, playback1, playback2)
        TestSpotifyInterceptor.intercept(Spotify.Player.SEEK_TO_POSITION_PATH, method = "PUT", Unit)
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_TRACKS_PATH, listOf(false))
        TestSpotifyInterceptor.intercept(Spotify.Library.CHECK_ALBUMS_PATH, listOf(false))

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = playback1, trackPlayback = null, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.SeekTo(positionMs = 100))

            presenter.assertStateEquals(loadedState.withPlayback(playback2))
            Player.verifyState(playback = playback2, trackPlayback = null, deviceId = null)
        }
    }

    @Test
    fun selectDevice() {
        val newDevice = device(id = "new device")
        TestSpotifyInterceptor.intercept(Spotify.Player.TRANSFER_PLAYBACK_PATH, method = "PUT", Unit)

        testPresenter(::PlayerPanelPresenter) { presenter ->
            verifyOpenState(playback = null, trackPlayback = null, deviceId = null)

            presenter.emitAndIdle(PlayerPanelPresenter.Event.SelectDevice(device = newDevice))

            presenter.assertStateEquals(loadedState.copy(selectedDevice = newDevice))
            // TODO probably should switch to the new device ID
            Player.verifyState(playback = null, trackPlayback = null, deviceId = null)
        }
    }

    private fun Player.verifyState(
        playback: SpotifyPlayback?,
        trackPlayback: SpotifyTrackPlayback?,
        deviceId: String?,
    ) {
        assertThat(isPlaying.value).isEqualTo(playback?.isPlaying == true || trackPlayback?.isPlaying == true)
        assertThat(playbackContext.value).isEqualTo(playback?.context ?: trackPlayback?.context)
        (playback?.item ?: trackPlayback?.item)?.let { track ->
            assertThat(currentTrackId.value).isEqualTo(track.id)
        }

        assertThat(currentPlaybackDeviceId.value).isEqualTo(deviceId)
    }

    private fun verifyOpenState(playback: SpotifyPlayback?, trackPlayback: SpotifyTrackPlayback?, deviceId: String?) {
        val openCalls = buildList {
            add(Spotify.Player.GET_AVAILABLE_DEVICES_PATH)
            add(Spotify.Player.GET_CURRENT_PLAYBACK_PATH)
            add(Spotify.Player.GET_CURRENT_PLAYING_TRACK_PATH)
            if (playback?.item != null) {
                add(Spotify.Library.CHECK_TRACKS_PATH)
                if (playback.item?.album != null) {
                    add(Spotify.Library.CHECK_ALBUMS_PATH)
                }
            }
            if (trackPlayback?.item != null) {
                add(Spotify.Library.CHECK_TRACKS_PATH)
                if (trackPlayback.item?.album != null) {
                    add(Spotify.Library.CHECK_ALBUMS_PATH)
                }
            }
        }

        TestSpotifyInterceptor.verifyInterceptedCalls(openCalls)
        Player.verifyState(playback, trackPlayback, deviceId)
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

        private const val deviceId = "device"
        private val device: SpotifyPlaybackDevice = device()
        private val track: FullSpotifyTrack = FixtureModels.networkFullTrack()
        private val trackA: FullSpotifyTrack = FixtureModels.networkFullTrack(id = "track-a", name = "Track A")
        private val trackB: FullSpotifyTrack = FixtureModels.networkFullTrack(id = "track-b", name = "Track B")

        private val playbackNotPlaying = playback(isPlaying = false, track = null)
        private val playbackPlaying = playback(isPlaying = true)

        private fun device(id: String = deviceId, volumePercent: Int = 50): SpotifyPlaybackDevice {
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

        private fun playback(
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

        private fun trackPlayback(
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
