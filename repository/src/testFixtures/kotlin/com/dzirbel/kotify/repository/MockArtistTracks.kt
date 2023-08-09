package com.dzirbel.kotify.repository

import com.dzirbel.kotify.repository.artist.ArtistTracksRepository
import io.mockk.every
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Mocks all calls to [ArtistTracksRepository.artistTracksStateOf] and [ArtistTracksRepository.artistTracksStatesOf] to
 * return a null set of track IDs.
 */
fun ArtistTracksRepository.mockArtistTracksNull() {
    every { artistTracksStateOf(any()) } returns MutableStateFlow(null)
    every { artistTracksStatesOf(any()) } answers {
        val n = firstArg<Iterable<String>>().count()
        List(n) { MutableStateFlow(null) }
    }
}
