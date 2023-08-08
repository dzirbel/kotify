package com.dzirbel.kotify.repository.player

import androidx.compose.runtime.Stable
import com.dzirbel.kotify.network.FullSpotifyTrackOrEpisode
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlaybackOffset
import com.dzirbel.kotify.network.model.SpotifyPlayingType
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.network.model.SpotifyTrackPlayback
import com.dzirbel.kotify.repository.album.AlbumViewModel
import com.dzirbel.kotify.repository.artist.ArtistViewModel
import com.dzirbel.kotify.repository.playlist.PlaylistViewModel
import com.dzirbel.kotify.repository.util.ToggleableState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Defines the public API for repository-level operations controlling the Spotify player state. In practice this is
 * always [PlayerRepository] and is only separated as in interface to make the API more clear and separate from the
 * implementation.
 */
@Stable
interface Player {
    /**
     * Encapsulates options to start playback.
     */
    @Stable
    data class PlayContext(
        val contextUri: String?,
        val offset: SpotifyPlaybackOffset? = null,
    ) {
        companion object {
            /**
             * Returns a [PlayContext] which plays the given [album].
             */
            fun album(album: SpotifyAlbum) = album.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the given [album].
             */
            fun album(album: AlbumViewModel) = album.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the track at the given [index] on the given [album].
             */
            fun albumTrack(album: AlbumViewModel, index: Int): PlayContext? {
                return album.uri?.let { uri ->
                    PlayContext(contextUri = uri, offset = SpotifyPlaybackOffset(position = index))
                }
            }

            fun artist(artist: ArtistViewModel) = artist.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the given [playlist].
             */
            fun playlist(playlist: PlaylistViewModel) = playlist.uri?.let { PlayContext(contextUri = it) }

            /**
             * Returns a [PlayContext] which plays the track at the given [index] on the given [playlist].
             */
            fun playlistTrack(playlist: PlaylistViewModel, index: Int): PlayContext? {
                return playlist.uri?.let {
                    PlayContext(contextUri = it, offset = SpotifyPlaybackOffset(position = index))
                }
            }
        }
    }

    /**
     * Whether the [SpotifyPlayback] state is currently being refreshed.
     */
    val refreshingPlayback: StateFlow<Boolean>

    /**
     * Whether the [SpotifyTrackPlayback] state is currently being refreshed.
     */
    val refreshingTrack: StateFlow<Boolean>

    /**
     * Whether the available [SpotifyPlaybackDevice]s state is currently being refreshed.
     */
    val refreshingDevices: StateFlow<Boolean>

    /**
     * Whether the player currently allows playback; null if not yet determined.
     */
    val playable: StateFlow<Boolean?>

    /**
     * Whether the player is currently playing (or being toggled to be playing/paused); null if not yet determined.
     */
    val playing: StateFlow<ToggleableState<Boolean>?>

    /**
     * The context URI (i.e. [PlayContext.contextUri]) of the current playback; null if not yet determined or not
     * playing.
     */
    val playbackContextUri: StateFlow<String?>

    /**
     * The current [SpotifyPlayingType] being played, or null if not yet determined.
     */
    val currentlyPlayingType: StateFlow<SpotifyPlayingType?>

    /**
     * Whether there is a track skip in progress.
     */
    val skipping: StateFlow<SkippingState>

    /**
     * The current [SpotifyRepeatMode] (not repeating, repeating song, or repeating context), or the mode being toggled
     * to; null if not yet determined.
     */
    val repeatMode: StateFlow<ToggleableState<SpotifyRepeatMode>?>

    /**
     * Whether or not the current playback is being shuffled, or the shuffle state being toggled to; null if not yet
     * determined.
     */
    val shuffling: StateFlow<ToggleableState<Boolean>?>

    /**
     * The current [FullSpotifyTrack] or [FullSpotifyEpisode] being played; null if no track is being played or not yet
     * determined.
     */
    val currentItem: StateFlow<FullSpotifyTrackOrEpisode?>

    /**
     * The last updated position on the [currentItem]; null if no track is being played or not yet determined.
     */
    val trackPosition: StateFlow<TrackPosition?>

    /**
     * The currently selected [SpotifyPlaybackDevice]; null if there is no current device or not yet determined.
     */
    val currentDevice: StateFlow<SpotifyPlaybackDevice?>

    /**
     * The list of [SpotifyPlaybackDevice]s available for playback; null if not yet determined.
     */
    val availableDevices: StateFlow<List<SpotifyPlaybackDevice>?>

    /**
     * The volume of the active playback device, or the volume it is being toggled to; null if there is no current
     * device or not yet determined.
     */
    val volume: StateFlow<ToggleableState<Int>?>

    /**
     * A live stream of any unexpected errors encountered by the [Player]. Exceptions thrown from network calls or
     * unexpected states will be caught to avoid disrupting the flow of player operations.
     */
    val errors: SharedFlow<Throwable>

    /**
     * Triggers an asynchronous refresh of the [SpotifyPlayback] state, if one is not already in progress.
     */
    fun refreshPlayback()

    /**
     * Triggers an asynchronous refresh of the [SpotifyTrackPlayback] state, if one is not already in progress.
     */
    fun refreshTrack()

    /**
     * Triggers an asynchronous refresh of the available [SpotifyPlaybackDevice]s, if one is not already in progress.
     */
    fun refreshDevices()

    /**
     * Asynchronously starts playback from the given [context] or resumes the current playback if null.
     */
    fun play(context: PlayContext? = null)

    /**
     * Asynchronously pauses the current playback.
     */
    fun pause()

    /**
     * Asynchronously skips to the next track.
     */
    fun skipToNext()

    /**
     * Asynchronously skips to the previous track.
     */
    fun skipToPrevious()

    /**
     * Asynchronously seeks to the given [positionMs] in the current track.
     */
    fun seekToPosition(positionMs: Int)

    /**
     * Asynchronously sets the current [SpotifyRepeatMode] to the given [mode].
     */
    fun setRepeatMode(mode: SpotifyRepeatMode)

    /**
     * Asynchronously sets the current shuffle state to [shuffle].
     */
    fun setShuffle(shuffle: Boolean)

    /**
     * Asynchronously sets the volume of the active playback device to the given [volumePercent], from 0 to 100.
     */
    fun setVolume(volumePercent: Int)

    /**
     * Asynchronously transfers playback to the given device with the given [deviceId]; if [play] is true then also
     * ensures the new device starts or resumes playback, otherwise keeps the current playback state.
     */
    fun transferPlayback(deviceId: String, play: Boolean? = null)
}
