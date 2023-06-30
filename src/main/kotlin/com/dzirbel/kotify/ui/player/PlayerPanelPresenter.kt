package com.dzirbel.kotify.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedArtistRepository
import com.dzirbel.kotify.db.model.SavedTrackRepository
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.db.model.TrackRatingRepository
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyTrack
import com.dzirbel.kotify.repository.Rating
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.util.zipToMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

class PlayerPanelPresenter(scope: CoroutineScope) :
    Presenter<PlayerPanelPresenter.ViewModel, PlayerPanelPresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.LoadDevices(), Event.LoadPlayback(), Event.LoadTrackPlayback()),
        initialState = ViewModel(),
    ) {

    @Stable // necessary due to use of StateFlow
    data class ViewModel(
        val playbackTrack: SpotifyTrack? = null,
        val playbackProgressMs: Long? = null,
        val playbackIsPlaying: Boolean? = null,
        val playbackShuffleState: Boolean? = null,
        val playbackRepeatState: String? = null,
        val playbackCurrentDevice: SpotifyPlaybackDevice? = null,

        val selectedDevice: SpotifyPlaybackDevice? = null,
        val devices: List<SpotifyPlaybackDevice>? = null,

        // non-null when muted, saves the previous volume percent
        val savedVolume: Int? = null,

        val trackSavedState: StateFlow<Boolean?>? = null,
        val artistSavedStates: Map<String, StateFlow<Boolean?>>? = null,
        val albumSavedState: StateFlow<Boolean?>? = null,

        val trackRatingState: State<Rating?>? = null,

        val loadingPlayback: Boolean = true,
        val loadingTrackPlayback: Boolean = true,
        val loadingDevices: Boolean = true,

        val togglingShuffle: Boolean = false,
        val togglingRepeat: Boolean = false,
        val togglingPlayback: Boolean = false,
        val skippingPrevious: Boolean = false,
        val skippingNext: Boolean = false,
    ) {
        val currentDevice: SpotifyPlaybackDevice?
            get() = selectedDevice ?: playbackCurrentDevice ?: devices?.firstOrNull()
    }

    sealed class Event {
        data class LoadDevices(
            val untilVolumeChange: Boolean = false,
            val untilVolumeChangeDeviceId: String? = null,
            val retries: Int = 5,
        ) : Event()

        data class LoadPlayback(
            val untilIsPlayingChange: Boolean = false,
            val untilShuffleStateChange: Boolean = false,
            val untilRepeatStateChange: Boolean = false,
            val retries: Int = 5,
        ) : Event()

        data class LoadTrackPlayback(val untilTrackChange: Boolean = false, val retries: Int = 5) : Event()

        object Play : Event()
        object Pause : Event()
        object SkipNext : Event()
        object SkipPrevious : Event()
        class ToggleShuffle(val shuffle: Boolean) : Event()
        class SetRepeat(val repeatState: String) : Event()
        class SetVolume(val volume: Int) : Event()
        class ToggleMuteVolume(val mute: Boolean, val previousVolume: Int) : Event()
        class SeekTo(val positionMs: Int) : Event()

        class SelectDevice(val device: SpotifyPlaybackDevice) : Event()

        class ToggleTrackSaved(val trackId: String, val save: Boolean) : Event()
        class ToggleAlbumSaved(val albumId: String, val save: Boolean) : Event()
        class ToggleArtistSaved(val artistId: String, val save: Boolean) : Event()

        class RateTrack(val trackId: String, val rating: Rating?) : Event()
    }

    override fun externalEvents(): Flow<Event> {
        return flow {
            Player.playEvents.collect { playEvent ->
                emit(Event.LoadPlayback(untilIsPlayingChange = true))
                if (playEvent.contextChanged) {
                    emit(Event.LoadTrackPlayback(untilTrackChange = true))
                }
            }
        }
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.LoadDevices -> {
                val previousVolume: Int?
                mutateState { state ->
                    previousVolume = event.untilVolumeChangeDeviceId?.let { deviceId ->
                        state.devices?.find { it.id == deviceId }?.volumePercent
                    }

                    state.copy(loadingDevices = true)
                }

                val devices = try {
                    Spotify.Player.getAvailableDevices()
                } catch (ex: Throwable) {
                    mutateState { it.copy(loadingDevices = false) }
                    throw ex
                }

                val expectedChangeDevice = event.untilVolumeChangeDeviceId?.let { deviceId ->
                    devices.find { it.id == deviceId }
                }

                @Suppress("ComplexCondition")
                if (event.untilVolumeChange &&
                    event.retries > 0 &&
                    expectedChangeDevice != null &&
                    expectedChangeDevice.volumePercent == previousVolume
                ) {
                    delay(REFRESH_BUFFER_MS)
                    emit(event.copy(retries = event.retries - 1))
                } else {
                    mutateState {
                        it.copy(devices = devices, loadingDevices = false)
                    }
                    Player.currentPlaybackDeviceId.value = queryState { it.currentDevice?.id }
                }
            }

            is Event.LoadPlayback -> {
                val previousIsPlaying: Boolean?
                val previousRepeatState: String?
                val previousShuffleState: Boolean?
                mutateState { state ->
                    previousIsPlaying = state.playbackIsPlaying
                    previousRepeatState = state.playbackRepeatState
                    previousShuffleState = state.playbackShuffleState

                    state.copy(loadingPlayback = true)
                }

                val playback = try {
                    Spotify.Player.getCurrentPlayback()
                } catch (ex: Throwable) {
                    mutateState { it.copy(loadingPlayback = false) }
                    throw ex
                }

                if (playback != null) {
                    Player.isPlaying.value = playback.isPlaying
                    Player.playbackContext.value = playback.context

                    playback.item?.let { track ->
                        Player.currentTrackId.value = track.id

                        // cache track in database or update stored data
                        KotifyDatabase.transaction(name = "save track ${track.name}") { Track.from(track) }
                    }
                }

                when {
                    playback == null -> mutateState { it.copy(loadingPlayback = false) }

                    event.untilIsPlayingChange && event.retries > 0 && playback.isPlaying == previousIsPlaying -> {
                        // try again until the playing state changes
                        delay(REFRESH_BUFFER_MS)
                        emit(event.copy(retries = event.retries - 1))
                    }

                    event.untilRepeatStateChange && event.retries > 0 &&
                        playback.repeatState == previousRepeatState -> {
                        // try again until the repeat state changes
                        delay(REFRESH_BUFFER_MS)
                        emit(event.copy(retries = event.retries - 1))
                    }

                    event.untilShuffleStateChange && event.retries > 0 &&
                        playback.shuffleState == previousShuffleState -> {
                        // try again until the shuffle state changes
                        delay(REFRESH_BUFFER_MS)
                        emit(event.copy(retries = event.retries - 1))
                    }

                    else -> {
                        val track = playback.item

                        val trackSavedState = track?.id?.let { trackId ->
                            SavedTrackRepository.stateOf(id = trackId)
                        }

                        val trackRatingState = track?.id?.let { trackId ->
                            TrackRatingRepository.ratingState(id = trackId)
                        }

                        val artistSavedStates = track?.artists?.mapNotNull { it.id }?.let { trackArtistIds ->
                            trackArtistIds.zipToMap(SavedArtistRepository.statesOf(ids = trackArtistIds))
                        }

                        val albumSavedState = track?.album?.id?.let {
                            SavedAlbumRepository.stateOf(id = it)
                        }

                        mutateState {
                            it.copy(
                                playbackTrack = track,
                                trackSavedState = trackSavedState,
                                trackRatingState = trackRatingState,
                                artistSavedStates = artistSavedStates,
                                albumSavedState = albumSavedState,
                                playbackProgressMs = playback.progressMs,
                                playbackIsPlaying = playback.isPlaying,
                                playbackShuffleState = playback.shuffleState,
                                playbackRepeatState = playback.repeatState,
                                playbackCurrentDevice = playback.device,
                                loadingPlayback = false,
                            )
                        }
                    }
                }
            }

            is Event.LoadTrackPlayback -> {
                val currentTrack: SpotifyTrack?
                mutateState { state ->
                    currentTrack = state.playbackTrack
                    state.copy(loadingTrackPlayback = true)
                }

                val trackPlayback = try {
                    Spotify.Player.getCurrentlyPlayingTrack()
                } catch (ex: Throwable) {
                    mutateState { it.copy(loadingTrackPlayback = false) }
                    throw ex
                }

                if (trackPlayback != null) {
                    Player.isPlaying.value = trackPlayback.isPlaying
                    Player.playbackContext.value = trackPlayback.context

                    trackPlayback.item?.let { track ->
                        Player.currentTrackId.value = track.id

                        // cache track in database or update stored data
                        KotifyDatabase.transaction(name = "save track ${track.name}") { Track.from(track) }
                    }
                }

                when {
                    trackPlayback == null ->
                        mutateState {
                            it.copy(
                                playbackTrack = null,
                                trackSavedState = null,
                                artistSavedStates = null,
                                albumSavedState = null,
                                loadingTrackPlayback = false,
                            )
                        }

                    trackPlayback.item == null ->
                        if (event.retries > 0) {
                            // try again until we get a valid track
                            delay(REFRESH_BUFFER_MS)

                            emit(
                                Event.LoadTrackPlayback(
                                    untilTrackChange = event.untilTrackChange,
                                    retries = event.retries - 1,
                                ),
                            )
                        } else {
                            mutateState { it.copy(loadingTrackPlayback = false) }
                        }

                    event.untilTrackChange && trackPlayback.item == currentTrack ->
                        if (event.retries > 0) {
                            // try again until the track changes
                            delay(REFRESH_BUFFER_MS)

                            emit(Event.LoadTrackPlayback(untilTrackChange = true, retries = event.retries - 1))
                        } else {
                            mutateState { it.copy(loadingTrackPlayback = false) }
                        }

                    else -> {
                        val track = checkNotNull(trackPlayback.item)

                        val trackSavedState = SavedTrackRepository.stateOf(id = track.id)
                        val trackRatingState = TrackRatingRepository.ratingState(id = track.id)

                        val trackArtistIds = track.artists.mapNotNull { it.id }
                        val artistSavedStates = trackArtistIds
                            .zipToMap(SavedArtistRepository.statesOf(ids = trackArtistIds))

                        val albumSavedState = track.album.id?.let {
                            SavedAlbumRepository.stateOf(id = it)
                        }

                        mutateState {
                            it.copy(
                                playbackTrack = track,
                                trackSavedState = trackSavedState,
                                trackRatingState = trackRatingState,
                                artistSavedStates = artistSavedStates,
                                albumSavedState = albumSavedState,
                                playbackProgressMs = trackPlayback.progressMs,
                                playbackIsPlaying = trackPlayback.isPlaying,
                                loadingTrackPlayback = false,
                            )
                        }

                        // refresh after the current track is expected to end
                        if (trackPlayback.isPlaying) {
                            val millisLeft = track.durationMs - trackPlayback.progressMs

                            delay(millisLeft + REFRESH_BUFFER_MS)

                            emit(Event.LoadTrackPlayback())
                        }
                    }
                }
            }

            Event.Play -> {
                val deviceId = queryState { it.currentDevice?.id }
                mutateState { it.copy(togglingPlayback = true) }

                try {
                    Spotify.Player.startPlayback(deviceId = deviceId)
                    emit(Event.LoadPlayback(untilIsPlayingChange = true))
                } finally {
                    mutateState { it.copy(togglingPlayback = false) }
                }
            }

            Event.Pause -> {
                val deviceId = queryState { it.currentDevice?.id }
                mutateState { it.copy(togglingPlayback = true) }

                try {
                    Spotify.Player.pausePlayback(deviceId = deviceId)
                    emit(Event.LoadPlayback(untilIsPlayingChange = true))
                } finally {
                    mutateState { it.copy(togglingPlayback = false) }
                }
            }

            Event.SkipNext -> {
                val deviceId = queryState { it.currentDevice?.id }
                mutateState { it.copy(skippingNext = true) }

                try {
                    Spotify.Player.skipToNext(deviceId = deviceId)
                    emit(Event.LoadTrackPlayback(untilTrackChange = true))
                } finally {
                    mutateState { it.copy(skippingNext = false) }
                }
            }

            Event.SkipPrevious -> {
                val deviceId = queryState { it.currentDevice?.id }
                mutateState { it.copy(skippingPrevious = true) }

                try {
                    Spotify.Player.skipToPrevious(deviceId = deviceId)
                    emit(Event.LoadTrackPlayback(untilTrackChange = true))
                } finally {
                    mutateState { it.copy(skippingPrevious = false) }
                }
            }

            is Event.ToggleShuffle -> {
                val deviceId = queryState { it.currentDevice?.id }
                mutateState { it.copy(togglingShuffle = true) }

                try {
                    Spotify.Player.toggleShuffle(deviceId = deviceId, state = event.shuffle)
                    emit(Event.LoadPlayback(untilShuffleStateChange = true))
                } finally {
                    mutateState { it.copy(togglingShuffle = false) }
                }
            }

            is Event.SetRepeat -> {
                val deviceId = queryState { it.currentDevice?.id }
                mutateState { it.copy(togglingRepeat = true) }

                try {
                    Spotify.Player.setRepeatMode(deviceId = deviceId, state = event.repeatState)
                    emit(Event.LoadPlayback(untilRepeatStateChange = true))
                } finally {
                    mutateState { it.copy(togglingRepeat = false) }
                }
            }

            is Event.SetVolume -> {
                val deviceId = queryState { it.currentDevice?.id }
                mutateState { it.copy(savedVolume = event.volume) }

                Spotify.Player.setVolume(deviceId = deviceId, volumePercent = event.volume)
                emit(Event.LoadDevices(untilVolumeChange = true, untilVolumeChangeDeviceId = deviceId))
            }

            is Event.ToggleMuteVolume -> {
                val deviceId = queryState { it.currentDevice?.id }
                val savedVolume = queryState { it.savedVolume }

                val volume = if (event.mute) 0 else requireNotNull(savedVolume) { "no saved volume" }
                Spotify.Player.setVolume(deviceId = deviceId, volumePercent = volume)
                emit(Event.LoadDevices(untilVolumeChange = true, untilVolumeChangeDeviceId = deviceId))

                mutateState { it.copy(savedVolume = event.previousVolume) }
            }

            is Event.SeekTo -> {
                val deviceId = queryState { it.currentDevice?.id }

                Spotify.Player.seekToPosition(deviceId = deviceId, positionMs = event.positionMs)

                // hack: wait a bit to ensure we have a load after the seek has come into effect, otherwise sometimes
                // the next playback load still has the old position
                delay(REFRESH_BUFFER_MS)
                emit(Event.LoadPlayback())
            }

            is Event.SelectDevice -> {
                val previousSelectedDevice = queryState { it.selectedDevice }
                mutateState { it.copy(selectedDevice = event.device) }

                try {
                    Spotify.Player.transferPlayback(deviceIds = listOf(event.device.id))
                } catch (ex: Throwable) {
                    mutateState { it.copy(selectedDevice = previousSelectedDevice) }
                    throw ex
                }
            }

            is Event.ToggleTrackSaved -> SavedTrackRepository.setSaved(id = event.trackId, saved = event.save)

            is Event.ToggleAlbumSaved -> SavedAlbumRepository.setSaved(id = event.albumId, saved = event.save)

            is Event.ToggleArtistSaved -> SavedArtistRepository.setSaved(id = event.artistId, saved = event.save)

            is Event.RateTrack -> TrackRatingRepository.rate(id = event.trackId, rating = event.rating)
        }
    }

    companion object {
        /**
         * A buffer in milliseconds after the current track is expected to end before fetching the next playback object,
         * to account for network time, etc.
         */
        private const val REFRESH_BUFFER_MS = 500L
    }
}
