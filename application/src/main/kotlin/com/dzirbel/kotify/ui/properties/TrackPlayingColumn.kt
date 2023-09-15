package com.dzirbel.kotify.ui.properties

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.model.Track
import com.dzirbel.kotify.network.model.SpotifyTrack
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.LocalPlayer
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.table.Column
import com.dzirbel.kotify.ui.components.table.ColumnWidth
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A [Column] which displays the current play state of a track of type [T] (abstract to support both actual tracks and
 * playlist tracks) with an icon, and allows playing a [SpotifyTrack] via the [playContextFromTrack].
 */
class TrackPlayingColumn<T>(
    private val trackIdOf: (track: T) -> String?,
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
        val trackId = trackIdOf(item) ?: return

        val hoverInteractionSource = remember { MutableInteractionSource() }
        val hovering = hoverInteractionSource.collectIsHoveredAsState()

        val player = LocalPlayer.current
        val currentTrackState = player.currentItem.collectAsState()
        val playing = remember(trackId) {
            derivedStateOf { currentTrackState.value?.id == trackId }
        }

        val size = Dimens.iconSmall
        Box(Modifier.padding(Dimens.space1).size(size).hoverable(hoverInteractionSource)) {
            if (playing.value) {
                CachedIcon(
                    name = "volume-up",
                    size = size,
                    contentDescription = "Playing",
                    tint = MaterialTheme.colors.primary,
                )
            } else {
                if (hovering.value) {
                    val context = playContextFromTrack(item)
                    IconButton(
                        onClick = { player.play(context = context) },
                        enabled = context != null,
                    ) {
                        CachedIcon(
                            name = "play-circle-outline",
                            size = size,
                            contentDescription = "Play",
                        )
                    }
                }
            }
        }
    }
}
