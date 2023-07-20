package com.dzirbel.kotify

import com.dzirbel.kotify.repository2.artist.ArtistRepository
import com.dzirbel.kotify.repository2.artist.SavedArtistRepository
import com.dzirbel.kotify.repository2.playlist.PlaylistRepository
import com.dzirbel.kotify.repository2.playlist.PlaylistTracksRepository
import com.dzirbel.kotify.repository2.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.repository2.rating.TrackRatingRepository
import com.dzirbel.kotify.repository2.track.SavedTrackRepository
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
