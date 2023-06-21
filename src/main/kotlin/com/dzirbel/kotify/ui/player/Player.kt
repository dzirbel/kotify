package com.dzirbel.kotify.ui.player

import androidx.compose.runtime.mutableStateOf
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyPlaybackContext
import com.dzirbel.kotify.network.model.SpotifyPlaybackOffset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
        val contextUri: String?,
        val trackUris: ImmutableList<String>? = null,
        val offset: SpotifyPlaybackOffset? = null,
        val positionMs: Int? = null,
    ) {
        companion object {
            /**
             * Returns a [PlayContext] which plays the given [album].
             */
            fun album(album: SpotifyAlbum) = album.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the given [album].
             */
            fun album(album: Album) = album.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the track at the given [index] on the given [album].
             */
            fun albumTrack(album: Album, index: Int): PlayContext? {
                return album.uri?.let { uri ->
                    PlayContext(contextUri = uri, offset = SpotifyPlaybackOffset(position = index))
                }
            }

            fun artist(artist: Artist) = artist.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the given [playlist].
             */
            fun playlist(playlist: Playlist) = playlist.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the track at the given [index] on the given [playlist].
             */
            fun playlistTrack(playlist: Playlist, index: Int): PlayContext? {
                return playlist.uri?.let {
                    PlayContext(contextUri = it, offset = SpotifyPlaybackOffset(position = index))
                }
            }

            /**
             * Returns a [PlayContext] which plays the given [track] with no context, i.e. plays only the track.
             */
            fun track(track: Track): PlayContext? {
                return track.uri?.let {
                    PlayContext(contextUri = null, trackUris = persistentListOf(it))
                }
            }
        }
    }

    private val _playEvents = MutableSharedFlow<PlayEvent>()

    /**
     * A [androidx.compose.runtime.MutableState] of the ID of the currently active playback device. [play] requests will
     * be sent to this device, and [playable] is true when it is non-null.
     */
    val currentPlaybackDeviceId = mutableStateOf<String?>(null)

    /**
     * A [androidx.compose.runtime.MutableState] of the current [SpotifyPlaybackContext].
     */
    val playbackContext = mutableStateOf<SpotifyPlaybackContext?>(null)

    /**
     * A [androidx.compose.runtime.MutableState] of whether the playback is currently playing.
     */
    val isPlaying = mutableStateOf(false)

    /**
     * A [androidx.compose.runtime.MutableState] of the ID of the currently playing [FullSpotifyTrack].
     */
    val currentTrackId = mutableStateOf<String?>(null)

    /**
     * Whether it is currently possible to play music in the player.
     */
    val playable: Boolean
        get() = currentPlaybackDeviceId.value != null

    /**
     * A [SharedFlow] which emits [Unit] each time [play] changes the playback.
     */
    val playEvents: SharedFlow<PlayEvent> = _playEvents.asSharedFlow()

    private var playerScope: CoroutineScope = GlobalScope

    /**
     * Plays from the given [context], returning true if this is possible (i.e. [playable] is true) or false if not.
     */
    fun play(
        context: PlayContext? = null,
        resumeIfSameContext: Boolean = true,
        scope: CoroutineScope = playerScope,
    ): Boolean {
        currentPlaybackDeviceId.value?.let { deviceId ->
            scope.launch {
                val contextChanged = context?.contextUri != playbackContext.value?.uri
                Spotify.Player.startPlayback(
                    contextUri = context?.contextUri?.takeIf {
                        context.offset != null || context.positionMs != null || !resumeIfSameContext || contextChanged
                    },
                    uris = context?.trackUris,
                    offset = context?.offset,
                    positionMs = context?.positionMs,
                    deviceId = deviceId,
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
    fun pause(scope: CoroutineScope = playerScope): Boolean {
        currentPlaybackDeviceId.value?.let { deviceId ->
            scope.launch {
                Spotify.Player.pausePlayback(deviceId = deviceId)

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
    fun togglePlayback(scope: CoroutineScope = playerScope): Boolean {
        return if (isPlaying.value) pause(scope = scope) else play(scope = scope)
    }

    /**
     * Resets the locally held state of the player, for use at the end of tests to ensure state does not carry over.
     */
    fun resetState() {
        currentPlaybackDeviceId.value = null
        playbackContext.value = null
        isPlaying.value = false
        currentTrackId.value = null
    }

    /**
     * Runs [block] with [scope] used as the [CoroutineScope] in which player operations are submitted. By default this
     * is the [GlobalScope] so that network requests are not constrained to any particular screen/etc, but for tests it
     * can be restricted to the test itself to prevent requests escaping the test context.
     */
    suspend fun withPlayerScope(scope: CoroutineScope, block: suspend () -> Unit) {
        playerScope = scope
        block()
        playerScope = GlobalScope
    }
}
