package com.dzirbel.kotify.repository

import com.dzirbel.kotify.network.FullSpotifyTrackOrEpisode
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlayingType
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.repository.player.SkippingState
import com.dzirbel.kotify.repository.player.TrackPosition
import com.dzirbel.kotify.repository.util.ToggleableState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePlayer(
    refreshingPlayback: Boolean = false,
    refreshingTrack: Boolean = false,
    refreshingDevices: Boolean = false,
    playable: Boolean? = null,
    playing: Boolean? = null,
    playbackContextUri: String? = null,
    currentlyPlayingType: SpotifyPlayingType? = null,
    skipping: SkippingState = SkippingState.NOT_SKIPPING,
    repeatMode: SpotifyRepeatMode? = null,
    shuffling: Boolean? = null,
    currentItem: FullSpotifyTrackOrEpisode? = null,
    trackPosition: TrackPosition? = null,
    currentDevice: SpotifyPlaybackDevice? = null,
    availableDevices: List<SpotifyPlaybackDevice>? = currentDevice?.let { listOf(it) },
    volume: Int? = null,
) : Player {

    override val refreshingPlayback = MutableStateFlow(refreshingPlayback)
    override val refreshingTrack = MutableStateFlow(refreshingTrack)
    override val refreshingDevices = MutableStateFlow(refreshingDevices)
    override val playable = MutableStateFlow(playable)
    override val playing = MutableStateFlow(playing?.let { ToggleableState.Set(it) })
    override val playbackContextUri = MutableStateFlow(playbackContextUri)
    override val currentlyPlayingType = MutableStateFlow(currentlyPlayingType)
    override val skipping = MutableStateFlow(skipping)
    override val repeatMode = MutableStateFlow(repeatMode?.let { ToggleableState.Set(it) })
    override val shuffling = MutableStateFlow(shuffling?.let { ToggleableState.Set(it) })
    override val currentItem = MutableStateFlow(currentItem)
    override val trackPosition = MutableStateFlow(trackPosition)
    override val currentDevice = MutableStateFlow(currentDevice)
    override val availableDevices = MutableStateFlow(availableDevices)
    override val volume = MutableStateFlow(volume?.let { ToggleableState.Set(it) })
    override val errors = MutableSharedFlow<Throwable>()

    override fun refreshPlayback() { }
    override fun refreshTrack() { }
    override fun refreshDevices() { }
    override fun play(context: Player.PlayContext?) { }
    override fun pause() { }
    override fun skipToNext() { }
    override fun skipToPrevious() { }
    override fun seekToPosition(positionMs: Int) { }
    override fun setRepeatMode(mode: SpotifyRepeatMode) { }
    override fun setShuffle(shuffle: Boolean) { }
    override fun setVolume(volumePercent: Int) { }
    override fun transferPlayback(deviceId: String, play: Boolean?) { }
}
