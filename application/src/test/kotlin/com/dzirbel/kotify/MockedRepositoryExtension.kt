package com.dzirbel.kotify

import com.dzirbel.kotify.repository.artist.ArtistAlbumsRepository
import com.dzirbel.kotify.repository.artist.ArtistRepository
import com.dzirbel.kotify.repository.artist.SavedArtistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistRepository
import com.dzirbel.kotify.repository.playlist.PlaylistTracksRepository
import com.dzirbel.kotify.repository.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository.rating.TrackRatingRepository
import com.dzirbel.kotify.repository.track.SavedTrackRepository
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class MockedRepositoryExtension : BeforeEachCallback, AfterEachCallback {
    // TODO finish
    // TODO extract?
    private val repositories = arrayOf(
        ArtistRepository,
        ArtistAlbumsRepository,
        PlaylistRepository,
        PlaylistTracksRepository,
        SavedArtistRepository,
        SavedPlaylistRepository,
        SavedTrackRepository,
        TrackRatingRepository,
    )

    override fun beforeEach(context: ExtensionContext?) {
        mockkObject(*repositories)
    }

    override fun afterEach(context: ExtensionContext?) {
        unmockkObject(*repositories)
    }
}
