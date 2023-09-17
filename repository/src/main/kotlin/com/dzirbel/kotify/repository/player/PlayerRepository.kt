package com.dzirbel.kotify.repository.player

import com.dzirbel.kotify.log.MutableLog
import com.dzirbel.kotify.log.asLog
import com.dzirbel.kotify.log.error
import com.dzirbel.kotify.log.info
import com.dzirbel.kotify.log.success
import com.dzirbel.kotify.log.warn
import com.dzirbel.kotify.network.FullSpotifyTrackOrEpisode
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlayingType
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.network.model.SpotifyTrackPlayback
import com.dzirbel.kotify.repository.DataSource
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.player.Player.PlayContext
import com.dzirbel.kotify.repository.util.BackoffStrategy
import com.dzirbel.kotify.repository.util.JobLock
import com.dzirbel.kotify.repository.util.ToggleableState
import com.dzirbel.kotify.repository.util.midpointTimestampToNow
import com.dzirbel.kotify.util.CurrentTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlayerRepository(private val scope: CoroutineScope) : Player {

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

    private val _currentTrack = MutableStateFlow<FullSpotifyTrackOrEpisode?>(null)
    override val currentItem: StateFlow<FullSpotifyTrackOrEpisode?>
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

    private var songEndJob: Job? = null

    private val _log = MutableLog<Unit>(name = "Player", scope = scope)

    override val log = _log.asLog { Repository.LogData(source = DataSource.REMOTE) }

    override fun refreshPlayback() {
        fetchPlaybackLock.launch(scope = scope) {
            _refreshingPlayback.value = true

            val start = CurrentTime.mark
            var error = false
            val playback = try {
                Spotify.Player.getCurrentPlayback()
            } catch (ex: CancellationException) {
                _refreshingPlayback.value = false
                throw ex
            } catch (throwable: Throwable) {
                error = true
                _errors.emit(throwable)
                _log.error(throwable, "Error refreshing playback", duration = start.elapsedNow())
                null
            }

            if (playback != null) {
                _log.success("Refreshed playback", duration = start.elapsedNow())
                updateWithPlayback(playback, fetchTimestamp = start.midpointTimestampToNow())
            } else if (!error) {
                _log.warn("Received null playback", duration = start.elapsedNow())
            }

            _refreshingPlayback.value = false
        }
    }

    override fun refreshTrack() {
        refreshTrackWithRetries(condition = null)
    }

    private suspend fun refreshTrackUntilChanged(previousTrack: FullSpotifyTrackOrEpisode?) {
        if (previousTrack == null) {
            val exception = IllegalStateException("Missing previous track")
            _log.error(exception)
            _errors.emit(exception)
        } else {
            refreshTrackWithRetries { newTrackPlayback ->
                // stop refreshing when there is a new track
                newTrackPlayback.item?.id != previousTrack.id
            }
        }
    }

    private fun refreshTrackWithRetries(
        attempt: Int = 0,
        backoffStrategy: BackoffStrategy = BackoffStrategy.default,
        condition: ((SpotifyTrackPlayback) -> Boolean)?,
    ) {
        fetchTrackPlaybackLock.launch(scope = scope) {
            _refreshingTrack.value = true

            val start = CurrentTime.mark
            val trackPlayback = try {
                Spotify.Player.getCurrentlyPlayingTrack()
            } catch (ex: CancellationException) {
                _refreshingTrack.value = false
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                null
            }
            val fetchTimestamp = start.midpointTimestampToNow()

            if (trackPlayback != null) {
                if (condition?.invoke(trackPlayback) == false) {
                    val delay = backoffStrategy.delayFor(attempt = attempt)
                    if (delay != null) {
                        _log.info(
                            title = "Expected track playback refresh condition not met after ${attempt + 1} retries, " +
                                "retrying after $delay",
                            duration = start.elapsedNow(),
                        )

                        // TODO consider multiple concurrent retry conditions
                        launch {
                            delay(delay)
                            refreshTrackWithRetries(
                                condition = condition,
                                attempt = attempt + 1,
                                backoffStrategy = backoffStrategy,
                            )
                        }
                    } else {
                        // retries have failed to meet the condition, now use the provided playback
                        _log.warn(
                            title = "Expected track playback refresh condition not met after ${attempt + 1} retries, " +
                                "using playback anyway",
                            duration = start.elapsedNow(),
                        )
                        updateWithTrackPlayback(trackPlayback, fetchTimestamp = fetchTimestamp)
                    }
                } else {
                    // condition is null or has been met, apply the new playback
                    _log.success("Refreshed track playback", duration = start.elapsedNow())
                    updateWithTrackPlayback(trackPlayback, fetchTimestamp = fetchTimestamp)
                }
            } else {
                // if there is no playback, stop retries regardless
                _log.warn("Received null track playback", duration = start.elapsedNow())
                updateWithTrackPlayback(null, fetchTimestamp = fetchTimestamp)
            }

            _refreshingTrack.value = false
        }
    }

    override fun refreshDevices() {
        fetchAvailableDevicesLock.launch(scope = scope) {
            _refreshingDevices.value = true

            val start = CurrentTime.mark
            val devices = try {
                Spotify.Player.getAvailableDevices()
            } catch (ex: CancellationException) {
                _refreshingDevices.value = false
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                _log.error(throwable, "Error refreshing devices", duration = start.elapsedNow())
                null
            }

            if (devices != null) {
                _log.success("Refreshed devices", duration = start.elapsedNow())
                updateWithDevices(devices)
            }

            _refreshingDevices.value = false
        }
    }

    override fun play(context: PlayContext?) {
        playLock.launch(scope = scope) {
            _playing.toggleTo(true) {
                val start = CurrentTime.mark

                // TODO check remote to see if context has changed before resuming from a null context?

                try {
                    Spotify.Player.startPlayback(contextUri = context?.contextUri, offset = context?.offset)
                } catch (ex: CancellationException) {
                    throw ex
                } catch (throwable: Throwable) {
                    _errors.emit(throwable)
                    _log.error(throwable, "Error starting playback", duration = start.elapsedNow())
                    throw throwable
                }

                _log.success("Started playback", duration = start.elapsedNow())

                val trackPosition = _trackPosition.value
                if (context == null && trackPosition is TrackPosition.Fetched) {
                    // resuming playback from a known position, optimistically set the track position (before updating
                    // from the remote below as well)
                    _trackPosition.value = trackPosition.play(playTimestamp = start.midpointTimestampToNow())
                }

                // verify applied by refreshing until playing, and also until the context matches if provided
                refreshTrackWithRetries {
                    it.isPlaying && (context == null || it.context?.uri == context.contextUri)
                }
            }

            updateSongEndJob()
        }
    }

    override fun pause() {
        playLock.launch(scope = scope) {
            _playing.toggleTo(false) {
                val start = CurrentTime.mark

                try {
                    Spotify.Player.pausePlayback()
                } catch (ex: CancellationException) {
                    throw ex
                } catch (throwable: Throwable) {
                    _errors.emit(throwable)
                    _log.error(throwable, "Error pausing playback", duration = start.elapsedNow())
                    throw throwable
                }

                _log.success("Paused playback", duration = start.elapsedNow())

                (_trackPosition.value as? TrackPosition.Fetched)?.let { position ->
                    _trackPosition.value = position.pause(pauseTimestamp = start.midpointTimestampToNow())
                }

                // verify applied by refreshing until not playing
                refreshTrackWithRetries { !it.isPlaying }
            }

            updateSongEndJob()
        }
    }

    override fun skipToNext() {
        skipLock.launch(scope = scope) {
            _skipping.value = SkippingState.SKIPPING_TO_NEXT
            songEndJob?.cancel()

            val previousTrack = _currentTrack.value

            val start = CurrentTime.mark

            val success = try {
                Spotify.Player.skipToNext()
                true
            } catch (ex: CancellationException) {
                _skipping.value = SkippingState.NOT_SKIPPING
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                _log.error(throwable, "Error skipping to next", duration = start.elapsedNow())
                false
            }

            if (success) {
                _log.success("Skipped to next", duration = start.elapsedNow())
                refreshTrackUntilChanged(previousTrack = previousTrack)
            }

            _skipping.value = SkippingState.NOT_SKIPPING
        }
    }

    override fun skipToPrevious() {
        skipLock.launch(scope = scope) {
            _skipping.value = SkippingState.SKIPPING_TO_PREVIOUS
            songEndJob?.cancel()

            val previousTrack = _currentTrack.value

            val start = CurrentTime.mark

            val success = try {
                Spotify.Player.skipToPrevious()
                true
            } catch (ex: CancellationException) {
                _skipping.value = SkippingState.NOT_SKIPPING
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                _log.error(throwable, "Error skipping to previous", duration = start.elapsedNow())
                false
            }

            if (success) {
                _log.success("Skipped to previous", duration = start.elapsedNow())
                refreshTrackUntilChanged(previousTrack = previousTrack)
            }

            _skipping.value = SkippingState.NOT_SKIPPING
        }
    }

    override fun seekToPosition(positionMs: Int) {
        seekLock.launch(scope = scope) {
            val previousProgress = _trackPosition.value
            _trackPosition.value = TrackPosition.Seeking(positionMs = positionMs)

            val start = CurrentTime.mark

            val success = try {
                Spotify.Player.seekToPosition(positionMs = positionMs)
                true
            } catch (ex: CancellationException) {
                _trackPosition.value = previousProgress
                throw ex
            } catch (throwable: Throwable) {
                _trackPosition.value = previousProgress
                _errors.emit(throwable)
                _log.error(throwable, "Error seeking to position", duration = start.elapsedNow())
                false
            }

            val end = CurrentTime.mark

            if (success) {
                _log.success("Seeked to position", duration = start.elapsedNow())

                // optimistically update the track position before refreshing from the remote below
                _trackPosition.value = TrackPosition.Fetched(
                    fetchedTimestamp = start.midpointTimestampToNow(),
                    fetchedPositionMs = positionMs,
                    playing = _playing.value?.value,
                )

                updateSongEndJob()

                // returns the range of progressMs the track could have if the seek was applied between the measured
                // start and end times (including a buffer for a time the remote takes to apply the seek, even after
                // returning from the endpoint)
                fun possibleProgressMsRange(): LongRange {
                    val now = CurrentTime.mark
                    val msSinceStart = (now - start).inWholeMilliseconds
                    val msSinceEnd = (now - end).inWholeMilliseconds
                    return (positionMs + msSinceEnd)..(positionMs + msSinceStart + SEEK_TO_POSITION_BUFFER_MS)
                }

                refreshTrackWithRetries(backoffStrategy = BackoffStrategy.failFast) {
                    it.progressMs in possibleProgressMsRange()
                }
            }
        }
    }

    override fun setRepeatMode(mode: SpotifyRepeatMode) {
        setRepeatModeLock.launch(scope = scope) {
            _repeatMode.toggleTo(mode) {
                val start = CurrentTime.mark

                try {
                    Spotify.Player.setRepeatMode(mode)
                } catch (ex: CancellationException) {
                    throw ex
                } catch (throwable: Throwable) {
                    _errors.emit(throwable)
                    _log.error(throwable, "Error setting repeat mode", duration = start.elapsedNow())
                    throw throwable
                }

                _log.success("Set repeat mode", duration = start.elapsedNow())

                // TODO verify applied?
            }
        }
    }

    override fun setShuffle(shuffle: Boolean) {
        toggleShuffleLock.launch(scope = scope) {
            _shuffling.toggleTo(shuffle) {
                val start = CurrentTime.mark

                try {
                    Spotify.Player.toggleShuffle(state = shuffle)
                } catch (ex: CancellationException) {
                    throw ex
                } catch (throwable: Throwable) {
                    _errors.emit(throwable)
                    _log.error(throwable, "Error setting shuffle", duration = start.elapsedNow())
                    throw throwable
                }

                _log.success("Set shuffle", duration = start.elapsedNow())

                // TODO verify applied?
            }
        }
    }

    override fun setVolume(volumePercent: Int) {
        setVolumeLock.launch(scope = scope) {
            _volume.toggleTo(volumePercent) {
                val start = CurrentTime.mark

                try {
                    Spotify.Player.setVolume(volumePercent = volumePercent)
                } catch (ex: CancellationException) {
                    throw ex
                } catch (throwable: Throwable) {
                    _errors.emit(throwable)
                    _log.error(throwable, "Error setting volume", duration = start.elapsedNow())
                    throw throwable
                }

                _log.success("Set volume", duration = start.elapsedNow())

                // TODO verify applied?
            }
        }
    }

    override fun transferPlayback(deviceId: String, play: Boolean?) {
        transferPlaybackLock.launch(scope = scope) {
            val start = CurrentTime.mark

            try {
                Spotify.Player.transferPlayback(deviceIds = listOf(deviceId), play = play)
            } catch (ex: CancellationException) {
                throw ex
            } catch (throwable: Throwable) {
                _errors.emit(throwable)
                _log.error(throwable, "Error transferring playback", duration = start.elapsedNow())
                throw throwable
            }

            _log.success("Transferred playback", duration = start.elapsedNow())

            _availableDevices.value
                ?.find { it.id == deviceId }
                ?.let { _currentDevice.value = it }

            // TODO often fetches playback before the change has been applied, resetting the current device
            refreshPlayback()
            refreshTrack()
        }
    }

    private fun updateWithPlayback(playback: SpotifyPlayback, fetchTimestamp: Long) {
        _playable.value = !playback.device.isRestricted
        _currentDevice.value = playback.device

        playback.progressMs?.let { progressMs ->
            _trackPosition.value = TrackPosition.Fetched(
                fetchedTimestamp = fetchTimestamp,
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

        updateSongEndJob()
    }

    private fun updateWithTrackPlayback(trackPlayback: SpotifyTrackPlayback?, fetchTimestamp: Long) {
        _currentTrack.value = trackPlayback?.item

        _trackPosition.value = trackPlayback?.let {
            TrackPosition.Fetched(
                fetchedTimestamp = fetchTimestamp,
                fetchedPositionMs = trackPlayback.progressMs.toInt(),
                playing = trackPlayback.isPlaying,
            )
        }

        _playing.value = trackPlayback?.let { ToggleableState.Set(trackPlayback.isPlaying) }

        _playbackContextUri.value = trackPlayback?.context?.uri
        _currentlyPlayingType.value = trackPlayback?.currentlyPlayingType

        updateSongEndJob()
    }

    private fun updateWithDevices(devices: List<SpotifyPlaybackDevice>) {
        _availableDevices.value = devices

        devices.firstOrNull { it.isActive }?.let { activeDevice ->
            _volume.value = ToggleableState.Set(activeDevice.volumePercent)
            _currentDevice.value = activeDevice
        }
    }

    private fun updateSongEndJob() {
        songEndJob?.cancel()

        val playing = _playing.value?.value == true
        val track = _currentTrack.value
        val progressMs = _trackPosition.value?.currentPositionMs
        if (playing && track != null && progressMs != null) {
            songEndJob = scope.launch {
                delay(track.durationMs - progressMs - SONG_END_BUFFER_MS)

                refreshTrackWithRetries(backoffStrategy = BackoffStrategy.songEnd) {
                    !it.isPlaying || it.item?.id != track.id
                }
            }
        }
    }

    private suspend fun <T> MutableStateFlow<ToggleableState<T>?>.toggleTo(value: T, block: suspend () -> Unit) {
        val previousValue = this.value
        this.value = ToggleableState.TogglingTo(value)

        val success = try {
            block()
            true
        } catch (ex: CancellationException) {
            this.value = previousValue
            throw ex
        } catch (throwable: Throwable) {
            _errors.emit(throwable)
            false
        }

        this.value = if (success) ToggleableState.Set(value) else previousValue
    }

    companion object {
        /**
         * Buffer time in milliseconds to permit when verifying that a seek was applied.
         */
        private const val SEEK_TO_POSITION_BUFFER_MS = 2_500

        /**
         * Negative (i.e. retrying is this much earlier than expected time) buffer time in milliseconds included in the
         * delay before fetching the next track on song end.
         */
        private const val SONG_END_BUFFER_MS = 1_000
    }
}
