package com.dzirbel.kotify.repository

import com.dzirbel.kotify.repository.album.SavedAlbumRepository
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository

val savedRepositories = arrayOf(
    SavedAlbumRepository,
    SavedArtistRepository,
    SavedPlaylistRepository,
    SavedTrackRepository,
)
