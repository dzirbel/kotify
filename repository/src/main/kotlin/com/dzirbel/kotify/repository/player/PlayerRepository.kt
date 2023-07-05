package com.dzirbel.kotify.repository.player

import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlayingType
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.network.model.SpotifyTrackPlayback
import com.dzirbel.kotify.repository.player.Player.PlayContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// TODO document
// TODO fetch next song when current one ends
object PlayerRepository : Player {
    private val _refreshingPlayback = MutableStateFlow(false)
    override val refreshingPlayback: StateFlow<Boolean>
        get() = _refreshingPlayback

    private val _refreshingTrack = MutableStateFlow(false)
    override val refreshingTrack: StateFlow<Boolean>
        get() = _refreshingTrack

    private val _refreshingDevices = MutableStateFlow(false)
    override val refreshingDevices: StateFlow<Boolean>
        get() = _refreshingDevices

    private val _playable = MutableStateFlow<Boolean?>(null)
    override val playable: StateFlow<Boolean?>
        get() = _playable

    private val _playing = MutableStateFlow<ToggleableState<Boolean>?>(null)
    override val playing: StateFlow<ToggleableState<Boolean>?>
        get() = _playing

    private val _playbackContextUri = MutableStateFlow<String?>(null)
    override val playbackContextUri: StateFlow<String?>
        get() = _playbackContextUri

    private val _currentlyPlayingType = MutableStateFlow<SpotifyPlayingType?>(null)
    override val currentlyPlayingType: StateFlow<SpotifyPlayingType?>
        get() = _currentlyPlayingType

    private val _skipping = MutableStateFlow(SkippingState.NOT_SKIPPING)
    override val skipping: StateFlow<SkippingState>
        get() = _skipping

    private val _repeatMode = MutableStateFlow<ToggleableState<SpotifyRepeatMode>?>(null)
    override val repeatMode: StateFlow<ToggleableState<SpotifyRepeatMode>?>
        get() = _repeatMode

    private val _shuffling = MutableStateFlow<ToggleableState<Boolean>?>(null)
    override val shuffling: StateFlow<ToggleableState<Boolean>?>
        get() = _shuffling

    private val _currentTrack = MutableStateFlow<FullSpotifyTrack?>(null)
    override val currentTrack: StateFlow<FullSpotifyTrack?>
        get() = _currentTrack

    private val _trackPosition = MutableStateFlow<TrackPosition?>(null)
    override val trackPosition: StateFlow<TrackPosition?>
        get() = _trackPosition

    private val _currentDevice = MutableStateFlow<SpotifyPlaybackDevice?>(null)
    override val currentDevice: StateFlow<SpotifyPlaybackDevice?>
        get() = _currentDevice

    private val _availableDevices = MutableStateFlow<List<SpotifyPlaybackDevice>?>(null)
    override val availableDevices: StateFlow<List<SpotifyPlaybackDevice>?>
        get() = _availableDevices

    private val _volume = MutableStateFlow<ToggleableState<Int>?>(null)
    override val volume: StateFlow<ToggleableState<Int>?>
        get() = _volume

    private val _errors = MutableSharedFlow<Throwable>(replay = 8)
    override val errors: SharedFlow<Throwable>
        get() = _errors

    private val fetchPlaybackLock = JobLock()
    private val fetchTrackPlaybackLock = JobLock()
    private val fetchAvailableDevicesLock = JobLock()

    private val playLock = JobLock()
    private val skipLock = JobLock()
    private val seekLock = JobLock()
    private val setRepeatModeLock = JobLock()
    private val toggleShuffleLock = JobLock()
    private val setVolumeLock = JobLock()
    private val transferPlaybackLock = JobLock()

    init {
        refreshPlayback()
        refreshTrack()
        refreshDevices()
    }

    override fun refreshPlayback() {
        fetchPlaybackLock.launch(scope = GlobalScope) {
            _refreshingPlayback.value = true

            val playback = try {
                Spotify.Player.getCurrentPlayback()
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                null
            }

            if (playback != null) {
                updateWithPlayback(playback)
            }

            _refreshingPlayback.value = false
        }
    }

    override fun refreshTrack() {
        fetchTrackPlaybackLock.launch(scope = GlobalScope) {
            _refreshingTrack.value = true

            val trackPlayback = try {
                Spotify.Player.getCurrentlyPlayingTrack()
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                null
            }

            if (trackPlayback != null) {
                updateWithTrackPlayback(trackPlayback)
            }

            _refreshingTrack.value = false
        }
    }

    override fun refreshDevices() {
        fetchAvailableDevicesLock.launch(scope = GlobalScope) {
            _refreshingDevices.value = true

            val devices = try {
                Spotify.Player.getAvailableDevices()
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                null
            }

            if (devices != null) {
                updateWithDevices(devices)
            }

            _refreshingDevices.value = false
        }
    }

    override fun play(context: PlayContext?) {
        playLock.launch(scope = GlobalScope) {
            _playing.toggleTo(true) {
                val start = System.currentTimeMillis()

                Spotify.Player.startPlayback(
                    contextUri = context?.contextUri?.takeIf { contextUri ->
                        context.offset != null || context.positionMs != null || contextUri != playbackContextUri.value
                    },
                    uris = context?.trackUris,
                    offset = context?.offset,
                    positionMs = context?.positionMs,
                )

                (_trackPosition.value as? TrackPosition.Fetched)?.let { position ->
                    _trackPosition.value = position.play(playTimestamp = start.midpoint())
                }

                context?.contextUri?.let { _playbackContextUri.value = it }

                // TODO verify applied?
            }
        }
    }

    override fun pause() {
        playLock.launch(scope = GlobalScope) {
            _playing.toggleTo(false) {
                val start = System.currentTimeMillis()

                Spotify.Player.pausePlayback()

                (_trackPosition.value as? TrackPosition.Fetched)?.let { position ->
                    _trackPosition.value = position.pause(pauseTimestamp = start.midpoint())
                }

                // TODO verify applied?
            }
        }
    }

    override fun skipToNext() {
        skipLock.launch(scope = GlobalScope) {
            _skipping.value = SkippingState.SKIPPING_TO_NEXT

            val success = try {
                Spotify.Player.skipToNext()
                true
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                false
            }

            if (success) {
                // TODO refreshes too fast before the change is applied
                refreshTrack()
            }

            _skipping.value = SkippingState.NOT_SKIPPING
        }
    }

    override fun skipToPrevious() {
        skipLock.launch(scope = GlobalScope) {
            _skipping.value = SkippingState.SKIPPING_TO_PREVIOUS

            val success = try {
                Spotify.Player.skipToPrevious()
                true
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                false
            }

            if (success) {
                // TODO refreshes too fast before the change is applied
                refreshTrack()
            }

            _skipping.value = SkippingState.NOT_SKIPPING
        }
    }

    override fun seekToPosition(positionMs: Int) {
        seekLock.launch(scope = GlobalScope) {
            val previousProgress = _trackPosition.value
            _trackPosition.value = TrackPosition.Seeking(positionMs = positionMs)

            val start = System.currentTimeMillis()

            val success = try {
                Spotify.Player.seekToPosition(positionMs = positionMs)
                true
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                _trackPosition.value = previousProgress
                false
            }

            if (success) {
                _trackPosition.value = TrackPosition.Fetched(
                    fetchedTimestamp = start.midpoint(),
                    fetchedPositionMs = positionMs,
                    playing = _playing.value?.value == true, // TODO ?
                )

                // refresh the track to get a more accurate progress indication
                // TODO often fetches before the new position has been applied
                refreshTrack()
            }
        }
    }

    override fun setRepeatMode(mode: SpotifyRepeatMode) {
        setRepeatModeLock.launch(scope = GlobalScope) {
            _repeatMode.toggleTo(mode) {
                Spotify.Player.setRepeatMode(mode)

                // TODO verify applied?
            }
        }
    }

    override fun setShuffle(shuffle: Boolean) {
        toggleShuffleLock.launch(scope = GlobalScope) {
            _shuffling.toggleTo(shuffle) {
                Spotify.Player.toggleShuffle(state = shuffle)

                // TODO verify applied?
            }
        }
    }

    override fun setVolume(volumePercent: Int) {
        setVolumeLock.launch(scope = GlobalScope) {
            _volume.toggleTo(volumePercent) {
                Spotify.Player.setVolume(volumePercent = volumePercent)

                // TODO verify applied?
            }
        }
    }

    override fun transferPlayback(deviceId: String, play: Boolean?) {
        transferPlaybackLock.launch(scope = GlobalScope) {
            val success = try {
                Spotify.Player.transferPlayback(deviceIds = listOf(deviceId), play = play)
                true
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                false
            }

            if (success) {
                _availableDevices.value
                    ?.find { it.id == deviceId }
                    ?.let { _currentDevice.value = it }

                // TODO often fetches playback before the change has been applied, resetting the current device
                refreshPlayback()
                refreshTrack()
            }
        }
    }

    private fun updateWithPlayback(playback: SpotifyPlayback) {
        _playable.value = !playback.device.isRestricted
        _currentDevice.value = playback.device

        playback.progressMs?.let { progressMs ->
            _trackPosition.value = TrackPosition.Fetched(
                fetchedTimestamp = playback.timestamp,
                fetchedPositionMs = progressMs.toInt(),
                playing = playback.isPlaying,
            )
        }

        _playing.value = ToggleableState.Set(playback.isPlaying)

        // do not reset playback item to null if missing from the playback, since it is a lower signal than from track
        // playback
        playback.item?.let { _currentTrack.value = it }

        // do not use playback actions for these since they don't specify which state is being toggled to
        _shuffling.value = ToggleableState.Set(playback.shuffleState)
        _repeatMode.value = ToggleableState.Set(value = playback.repeatState)

        _playbackContextUri.value = playback.context?.uri
        _currentlyPlayingType.value = playback.currentlyPlayingType

        _skipping.value = when {
            playback.actions?.skippingNext == true -> SkippingState.SKIPPING_TO_NEXT
            playback.actions?.skippingPrev == true -> SkippingState.SKIPPING_TO_PREVIOUS
            else -> SkippingState.NOT_SKIPPING
        }
    }

    private fun updateWithTrackPlayback(trackPlayback: SpotifyTrackPlayback) {
        _currentTrack.value = trackPlayback.item

        _trackPosition.value = TrackPosition.Fetched(
            fetchedTimestamp = trackPlayback.timestamp,
            fetchedPositionMs = trackPlayback.progressMs.toInt(),
            playing = trackPlayback.isPlaying,
        )

        _playing.value = ToggleableState.Set(trackPlayback.isPlaying)

        _playbackContextUri.value = trackPlayback.context?.uri
        _currentlyPlayingType.value = trackPlayback.currentlyPlayingType
    }

    private fun updateWithDevices(devices: List<SpotifyPlaybackDevice>) {
        _availableDevices.value = devices

        devices.firstOrNull { it.isActive }?.let { activeDevice ->
            _volume.value = ToggleableState.Set(activeDevice.volumePercent)
            _currentDevice.value = activeDevice
        }
    }

    // TODO document
    private fun Long.midpoint(): Long = System.currentTimeMillis() / 2 + this / 2

    // TODO document
    private suspend fun <T> MutableStateFlow<ToggleableState<T>?>.toggleTo(value: T, block: suspend () -> Unit) {
        val previousValue = this.value
        this.value = ToggleableState.TogglingTo(value)

        val success = try {
            block()
            true
        } catch (ex: CancellationException) {
            throw ex
        } catch (throwable: Throwable) {
            _errors.emit(throwable)
            false
        }

        this.value = if (success) ToggleableState.Set(value) else previousValue
    }
}
