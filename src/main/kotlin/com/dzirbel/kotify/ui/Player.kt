package com.dzirbel.kotify.ui

import androidx.compose.runtime.mutableStateOf
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyArtist
import com.dzirbel.kotify.network.model.SpotifyPlaybackContext
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlaylist
import com.dzirbel.kotify.network.model.SpotifyTrack
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
    data class PlayEvent(val contextChanged: Boolean)

    /**
     * Encapsulates options to start playback.
     */
    data class PlayContext(
        val contextUri: String,
        val offset: Spotify.PlaybackOffset? = null,
        val positionMs: Int? = null,
    ) {
        interface Creator {
            fun create(track: SpotifyTrack, index: Int): PlayContext?
        }

        companion object {
            /**
             * Returns a [PlayContext] which plays the given [album].
             */
            fun album(album: SpotifyAlbum) = album.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the track at the given [index] on the given [album].
             */
            fun albumTrack(album: SpotifyAlbum, index: Int): PlayContext? {
                return album.uri?.let { uri ->
                    PlayContext(contextUri = uri, offset = Spotify.PlaybackOffset(position = index))
                }
            }

            fun artist(artist: SpotifyArtist) = artist.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the given [playlist].
             */
            fun playlist(playlist: SpotifyPlaylist) = PlayContext(contextUri = playlist.uri)

            /**
             * Returns a [PlayContext] which plays the track at the given [index] on the given [playlist].
             */
            fun playlistTrack(playlist: SpotifyPlaylist, index: Int): PlayContext {
                return PlayContext(contextUri = playlist.uri, offset = Spotify.PlaybackOffset(position = index))
            }
        }
    }

    private val _playEvents = MutableSharedFlow<PlayEvent>()

    /**
     * A [androidx.compose.runtime.MutableState] of the currently active [SpotifyPlaybackDevice]. [play] requests will
     * be sent to this device, and [playable] is true when it is non-null.
     */
    val currentDevice = mutableStateOf<SpotifyPlaybackDevice?>(null)

    /**
     * A [androidx.compose.runtime.MutableState] of the current [SpotifyPlaybackContext].
     */
    val playbackContext = mutableStateOf<SpotifyPlaybackContext?>(null)

    /**
     * A [androidx.compose.runtime.MutableState] of whether the playback is currently playing.
     */
    val isPlaying = mutableStateOf(false)

    /**
     * A [androidx.compose.runtime.MutableState] of the currently playing [FullSpotifyTrack].
     */
    val currentTrack = mutableStateOf<FullSpotifyTrack?>(null)

    /**
     * Whether it is currently possible to play music in the player.
     */
    val playable: Boolean
        get() = currentDevice.value != null

    /**
     * A [SharedFlow] which emits [Unit] each time [play] changes the playback.
     */
    val playEvents: SharedFlow<PlayEvent> = _playEvents.asSharedFlow()

    /**
     * Plays from the given [context], returning true if this is possible (i.e. [playable] is true) or false if not.
     */
    fun play(
        context: PlayContext? = null,
        resumeIfSameContext: Boolean = true,
        scope: CoroutineScope = GlobalScope
    ): Boolean {
        currentDevice.value?.let { device ->
            scope.launch {
                val contextChanged = context?.contextUri != playbackContext.value?.uri
                Spotify.Player.startPlayback(
                    contextUri = context?.contextUri?.takeUnless {
                        context.offset == null && context.positionMs == null && resumeIfSameContext && !contextChanged
                    },
                    offset = context?.offset,
                    positionMs = context?.positionMs,
                    deviceId = device.id
                )

                _playEvents.emit(PlayEvent(contextChanged = contextChanged))
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

                _playEvents.emit(PlayEvent(contextChanged = false))
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
