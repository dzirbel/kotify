package com.dominiczirbel.ui

import androidx.compose.runtime.mutableStateOf
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.network.model.FullTrack
import com.dominiczirbel.network.model.PlaybackContext
import com.dominiczirbel.network.model.PlaybackDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * A global object to expose the state of the Spotify player and allow changing the state from anywhere in the UI.
 */
object Player {
    private val _playEvents = MutableSharedFlow<Unit>()

    /**
     * A [androidx.compose.runtime.MutableState] of the currently active [PlaybackDevice]. [play] requests will be sent
     * to this device, and [playable] is true when it is non-null.
     */
    val currentDevice = mutableStateOf<PlaybackDevice?>(null)

    /**
     * A [androidx.compose.runtime.MutableState] of the current [PlaybackContext].
     */
    val playbackContext = mutableStateOf<PlaybackContext?>(null)

    /**
     * A [androidx.compose.runtime.MutableState] of whether the playback is currently playing.
     */
    val isPlaying = mutableStateOf(false)

    /**
     * A [androidx.compose.runtime.MutableState] of the currently playing [FullTrack].
     */
    val currentTrack = mutableStateOf<FullTrack?>(null)

    /**
     * Whether it is currently possible to play music in the player.
     */
    val playable: Boolean
        get() = currentDevice.value != null

    /**
     * A [SharedFlow] which emits [Unit] each time [play] changes the playback.
     */
    val playEvents: SharedFlow<Unit> = _playEvents.asSharedFlow()

    /**
     * Plays from the given [contextUri], returning true if this is possible (i.e. [playable] is true) or false if not.
     */
    fun play(
        contextUri: String? = null,
        resumeIfSameContext: Boolean = true,
        scope: CoroutineScope = GlobalScope
    ): Boolean {
        currentDevice.value?.let { device ->
            scope.launch {
                Spotify.Player.startPlayback(
                    contextUri = contextUri
                        ?.takeUnless { resumeIfSameContext && playbackContext.value?.uri == contextUri },
                    deviceId = device.id
                )

                _playEvents.emit(Unit)
            }
            return true
        }

        return false
    }

    /**
     * Pauses the current playback, returning true if this is possible (i.e. [playable] is true) or false if not.
     */
    fun pause(scope: CoroutineScope = GlobalScope): Boolean {
        currentDevice.value?.let { device ->
            scope.launch {
                Spotify.Player.pausePlayback(deviceId = device.id)

                _playEvents.emit(Unit)
            }
            return true
        }

        return false
    }

    /**
     * Toggles the current playback, pausing if it is playing and resuming if it is paused, returning true on success or
     * false on failure.
     */
    fun togglePlayback(scope: CoroutineScope = GlobalScope): Boolean {
        return if (isPlaying.value) pause(scope = scope) else play(scope = scope)
    }
}
