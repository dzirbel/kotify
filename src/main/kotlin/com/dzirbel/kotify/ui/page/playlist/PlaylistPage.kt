package com.dzirbel.kotify.ui.page.playlist

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.PlaylistTrack
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.trackColumns
import com.dzirbel.kotify.ui.framework.StandardPage
import com.dzirbel.kotify.ui.framework.rememberPresenter
import com.dzirbel.kotify.ui.player.Player

data class PlaylistPage(val playlistId: String) : Page<Playlist?> {
    @Composable
    override fun BoxScope.bind(visible: Boolean, toggleNavigationTitle: (Boolean) -> Unit): Playlist? {
        val presenter = rememberPresenter(key = playlistId) { scope -> PlaylistPresenter(playlistId, scope) }
        val scrollState = rememberScrollState()
        val stateOrError = presenter.state()
        val state = stateOrError.safeState

        if (visible) {
            // TODO refactor to avoid dependencies on state/etc and use BindPresenterPage instead
            val columns = remember(playlistId) {
                trackColumns(
                    savedTracks = state.savedTracksState?.value,
                    onSetTrackSaved = { trackId, saved ->
                        presenter.emitAsync(
                            PlaylistPresenter.Event.ToggleTrackSaved(trackId = trackId, saved = saved)
                        )
                    },
                    trackRatings = state.trackRatings,
                    onRateTrack = { trackId, rating ->
                        presenter.emitAsync(PlaylistPresenter.Event.RateTrack(trackId = trackId, rating = rating))
                    },
                    includeTrackNumber = false,
                    playContextFromIndex = { index ->
                        Player.PlayContext.playlistTrack(playlist = state.playlist!!, index = index)
                    }
                )
                    .map { column -> column.mapped<PlaylistTrack> { it.track.cached } }
                    .toMutableList()
                    .apply {
                        add(1, PlaylistTrackIndexColumn)

                        @Suppress("MagicNumber")
                        add(6, AddedAtColumn)
                    }
            }

            StandardPage(
                presenter = presenter,
                stateOrError = stateOrError,
                scrollState = scrollState,
                onHeaderVisibilityChanged = { toggleNavigationTitle(!it) },
                header = { PlaylistPageHeader(presenter, it, columns) },
                content = { PlaylistPageContent(presenter, it, columns) }
            )
        }

        return state.playlist
    }

    override fun titleFor(data: Playlist?) = data?.name
}
