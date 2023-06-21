package com.dzirbel.kotify.ui.properties

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.model.SpotifyTrack
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.hoverState
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.ColumnWidth
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors

/**
 * A [Column] which displays the current play state of a track of type [T] (abstract to support both actual tracks and
 * playlist tracks) with an icon, and allows playing a [SpotifyTrack] via the [playContextFromTrack].
 */
class TrackPlayingColumn<T>(
    private val trackIdOf: (track: T) -> String,
    /**
     * Returns a [Player.PlayContext] to play when the user selects the given [Track].
     */
    private val playContextFromTrack: (track: T) -> Player.PlayContext?,
) : Column<T> {
    override val title = "Currently playing"
    override val width = ColumnWidth.Fill()
    override val cellAlignment = Alignment.Center

    @Composable
    override fun Header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
        Box(Modifier)
    }

    @Composable
    override fun Item(item: T) {
        val hoverState = remember { mutableStateOf(false) }
        Box(Modifier.hoverState(hoverState).padding(Dimens.space2).size(Dimens.fontBodyDp)) {
            if (Player.currentTrackId.value == trackIdOf(item)) {
                CachedIcon(
                    name = "volume-up",
                    size = Dimens.fontBodyDp,
                    contentDescription = "Playing",
                    tint = LocalColors.current.primary,
                )
            } else {
                if (hoverState.value) {
                    val context = playContextFromTrack(item)
                    IconButton(
                        onClick = { Player.play(context = context) },
                        enabled = context != null,
                    ) {
                        CachedIcon(
                            name = "play-circle-outline",
                            size = Dimens.fontBodyDp,
                            contentDescription = "Play",
                            tint = LocalColors.current.primary,
                        )
                    }
                }
            }
        }
    }
}
