package com.dzirbel.kotify.ui.page.playlist

import com.dzirbel.kotify.FixtureModels
import com.dzirbel.kotify.testTransaction
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.util.RelativeTimeInfo
import org.junit.jupiter.api.Test
import java.time.Instant

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
        val now = Instant.now()
        val playlist = FixtureModels.playlist(fullUpdateTime = now)

        testTransaction {
            playlist.largestImage.loadToCache()
            playlist.playlistTracksInOrder.loadToCache()
            playlist.playlistTracksInOrder.cached.forEach { playlistTrack ->
                playlistTrack.track.loadToCache()
                playlistTrack.track.cached.artists.loadToCache()
            }
        }

        val baseState = PlaylistPresenter.ViewModel()
        val state = baseState.copy(
            playlist = playlist,
            tracks = baseState.tracks.withElements(playlist.playlistTracksInOrder.cached),
        )

        RelativeTimeInfo.withMockedTime(now) {
            screenshotTest(filename = "full", windowWidth = 1500) {
                PlaylistPage(playlistId = playlist.id.value).renderState(state)
            }
        }
    }
}
