package com.dzirbel.kotify.ui.page.playlist

import com.dzirbel.kotify.DatabaseExtension
import com.dzirbel.kotify.FixtureModels
import com.dzirbel.kotify.testTransaction
import com.dzirbel.kotify.ui.screenshotTest
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(DatabaseExtension::class)
internal class PlaylistPageScreenshotTest {
    @Test
    fun empty() {
        val state = PlaylistPresenter.ViewModel()

        screenshotTest(filename = "empty") {
            PlaylistPage(playlistId = "id").renderState(state)
        }
    }

    @Test
    fun full() {
        val playlist = spyk(FixtureModels.playlist())

        // hack: ensure updated time is rendered consistently as now
        every { playlist.updatedTime } answers { Instant.now() }

        testTransaction {
            playlist.largestImage.loadToCache()
            playlist.playlistTracksInOrder.loadToCache()
            playlist.playlistTracksInOrder.cached.forEach {
                it.track.loadToCache()
                it.track.cached.artists.loadToCache()
            }
        }

        val baseState = PlaylistPresenter.ViewModel()
        val state = baseState.copy(
            playlist = playlist,
            tracks = baseState.tracks.withElements(playlist.playlistTracksInOrder.cached),
        )

        screenshotTest(filename = "full", windowWidth = 1500) {
            PlaylistPage(playlistId = playlist.id.value).renderState(state)
        }
    }
}
