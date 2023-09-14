package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository.player.Player
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.LocalPlayer
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.derived
import com.dzirbel.kotify.ui.util.instrumentation.instrument

@Composable
fun PlayButton(context: Player.PlayContext?, size: Dp = Dimens.iconMedium) {
    val player = LocalPlayer.current

    val currentContextState = player.playbackContextUri.collectAsState()
    val playingState = player.playing.collectAsState()
    val playable = player.playable.collectAsState().derived { it == true }.value

    val matchesContext = remember(context?.contextUri) {
        derivedStateOf { currentContextState.value == context?.contextUri }
    }
    val playing = remember(context?.contextUri) {
        derivedStateOf { playingState.value?.value == true && matchesContext.value }
    }

    IconButton(
        enabled = playable && context != null,
        modifier = Modifier.instrument().size(size),
        onClick = {
            if (playing.value) player.pause() else player.play(context = context)
        },
    ) {
        CachedIcon(
            name = if (playing.value) "pause-circle-outline" else "play-circle-outline",
            size = size,
            contentDescription = "Play",
            tint = KotifyColors.highlighted(highlight = matchesContext.value),
        )
    }
}
