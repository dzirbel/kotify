package com.dzirbel.kotify.network

import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlayingType
import com.dzirbel.kotify.network.model.SpotifyRepeatMode

fun SpotifyPlayback(track: FullSpotifyTrack? = null): SpotifyPlayback {
    return SpotifyPlayback(
        item = track,
        device = SpotifyPlaybackDevice(),
        progressMs = null,
        isPlaying = false,
        shuffleState = false,
        repeatState = SpotifyRepeatMode.OFF,
        context = null,
        currentlyPlayingType = SpotifyPlayingType.UNKNOWN,
        actions = null,
        timestamp = 0,
    )
}

fun SpotifyPlaybackDevice(id: String = "device1", name: String = "Device 1"): SpotifyPlaybackDevice {
    return SpotifyPlaybackDevice(
        id = id,
        isActive = false,
        isRestricted = false,
        isPrivateSession = null,
        name = name,
        type = "type",
        volumePercent = 0,
    )
}
