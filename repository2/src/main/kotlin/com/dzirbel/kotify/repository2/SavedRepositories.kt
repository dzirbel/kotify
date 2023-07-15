package com.dzirbel.kotify.repository2

import com.dzirbel.kotify.repository2.album.SavedAlbumRepository
import com.dzirbel.kotify.repository2.artist.SavedArtistRepository
import com.dzirbel.kotify.repository2.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository2.track.SavedTrackRepository

// TODO drop "2" when :repository is merged in
val savedRepositories2 = arrayOf(
    SavedAlbumRepository,
    SavedArtistRepository,
    SavedPlaylistRepository,
    SavedTrackRepository,
)
