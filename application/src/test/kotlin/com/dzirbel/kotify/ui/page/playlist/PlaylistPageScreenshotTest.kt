package com.dzirbel.kotify.ui.page.playlist

import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.blockingTransaction
import com.dzirbel.kotify.repository.Playlist
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.ui.util.RelativeTimeInfo
import org.junit.jupiter.api.Test
import java.time.Instant

internal class PlaylistPageScreenshotTest {
    @Test
    fun empty() {
        val state = PlaylistPresenter.ViewModel()

        screenshotTest(filename = "empty") {
            PlaylistPage(playlistId = "id").RenderState(state)
        }
    }

    @Test
    fun full() {
        val now = Instant.now()
        val playlist = Playlist(fullUpdateTime = now)

        KotifyDatabase.blockingTransaction {
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
                PlaylistPage(playlistId = playlist.id.value).RenderState(state)
            }
        }
    }
}
