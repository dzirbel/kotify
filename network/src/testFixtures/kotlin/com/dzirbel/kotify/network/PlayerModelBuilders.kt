package com.dzirbel.kotify.network

import com.dzirbel.kotify.network.model.FullSpotifyTrack
import com.dzirbel.kotify.network.model.SpotifyPlayback
import com.dzirbel.kotify.network.model.SpotifyPlaybackDevice
import com.dzirbel.kotify.network.model.SpotifyPlayingType
import com.dzirbel.kotify.network.model.SpotifyRepeatMode
import com.dzirbel.kotify.network.model.SpotifyTrackPlayback

fun SpotifyPlayback(track: FullSpotifyTrack? = null, progressMs: Long? = null): SpotifyPlayback {
    return SpotifyPlayback(
        item = track,
        device = SpotifyPlaybackDevice(),
        progressMs = progressMs,
        isPlaying = false,
        shuffleState = false,
        repeatState = SpotifyRepeatMode.OFF,
        context = null,
        currentlyPlayingType = SpotifyPlayingType.UNKNOWN,
        actions = null,
        timestamp = 0,
    )
}

fun SpotifyTrackPlayback(track: FullSpotifyTrack? = null, progressMs: Long = 0): SpotifyTrackPlayback {
    return SpotifyTrackPlayback(
        item = track,
        progressMs = progressMs,
        isPlaying = false,
        context = null,
        currentlyPlayingType = SpotifyPlayingType.UNKNOWN,
        actions = null,
        timestamp = 0,
    )
}

fun SpotifyPlaybackDevice(id: String = "1", name: String = "Device $id"): SpotifyPlaybackDevice {
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
