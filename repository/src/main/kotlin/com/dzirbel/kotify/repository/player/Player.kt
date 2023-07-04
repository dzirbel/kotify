package com.dzirbel.kotify.repository.player

import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.repository.Player
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// TODO document
interface Player {
    val refreshingPlayback: StateFlow<Boolean>
    val refreshingTrack: StateFlow<Boolean>
    val refreshingDevices: StateFlow<Boolean>

    val playable: StateFlow<Boolean?>

    val playing: StateFlow<ToggleableState<Boolean>?>
    val playbackContextUri: StateFlow<String?>
    val currentlyPlayingType: StateFlow<String?>
    val skipping: StateFlow<SkippingState?>

    val repeatMode: StateFlow<ToggleableState<String>?>
    val shuffling: StateFlow<ToggleableState<Boolean>?>

    val currentTrack: StateFlow<FullSpotifyTrack?>
    val trackPosition: StateFlow<TrackPosition?>

    val currentDevice: StateFlow<SpotifyPlaybackDevice?>
    val availableDevices: StateFlow<List<SpotifyPlaybackDevice>?>
    val volume: StateFlow<ToggleableState<Int>?>

    val errors: SharedFlow<Throwable>

    fun refreshPlayback()
    fun refreshTrack()
    fun refreshDevices()

    // resume or play from new context
    // TODO move PlayContext here
    fun play(context: Player.PlayContext? = null)
    fun pause()

    fun togglePlayback() {
        val playingOrNull = (playing.value as? ToggleableState.Set)?.value
        if (playingOrNull != null) {
            if (playingOrNull) pause() else play()
        }
    }

    fun skipToNext()
    fun skipToPrevious()

    fun seekToPosition(positionMs: Int)

    fun setRepeatMode(mode: String)

    fun cycleRepeatMode() {
        repeatMode.value?.value?.let { currentRepeatMode ->
            // TODO extract repeat mode cycle logic / transfer to an enum
            val nextRepeatMode = when (currentRepeatMode) {
                "off" -> "todo"
                "track" -> "todo"
                "context" -> "todo"
                else -> "todo"
            }

            setRepeatMode(mode = nextRepeatMode)
        }
    }

    fun setShuffle(shuffle: Boolean)

    fun toggleShuffle() {
        shuffling.value?.value?.let { currentShuffle ->
            setShuffle(shuffle = !currentShuffle)
        }
    }

    // TODO convenience methods for mute / unmute?
    fun setVolume(volumePercent: Int)

    fun transferPlayback(deviceId: String, play: Boolean? = null)
}
