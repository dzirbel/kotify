package com.dzirbel.kotify.repository.player

import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyAlbum
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlaybackOffset
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// TODO document
interface Player {
    /**
     * Encapsulates options to start playback.
     */
    data class PlayContext(
        val contextUri: String?,
        val trackUris: List<String>? = null,
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
                    PlayContext(contextUri = null, trackUris = listOf(it))
                }
            }
        }
    }

    val refreshingPlayback: StateFlow<Boolean>
    val refreshingTrack: StateFlow<Boolean>
    val refreshingDevices: StateFlow<Boolean>

    val playable: StateFlow<Boolean?>

    val playing: StateFlow<ToggleableState<Boolean>?>
    val playbackContextUri: StateFlow<String?>
    val currentlyPlayingType: StateFlow<String?>
    val skipping: StateFlow<SkippingState?>

    val repeatMode: StateFlow<ToggleableState<SpotifyRepeatMode>?>
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
    fun play(context: PlayContext? = null)
    fun pause()

    fun skipToNext()
    fun skipToPrevious()

    fun seekToPosition(positionMs: Int)

    fun setRepeatMode(mode: SpotifyRepeatMode)

    fun setShuffle(shuffle: Boolean)

    fun setVolume(volumePercent: Int)

    fun transferPlayback(deviceId: String, play: Boolean? = null)
}
