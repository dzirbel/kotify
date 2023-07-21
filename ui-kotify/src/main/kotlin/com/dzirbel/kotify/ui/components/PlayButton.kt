package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.repository2.player.Player
import com.dzirbel.kotify.repository2.player.PlayerRepository
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.derived
import com.dzirbel.kotify.ui.util.instrumentation.instrument

@Composable
fun PlayButton(context: Player.PlayContext?, size: Dp = Dimens.iconMedium) {
    val currentContextState = PlayerRepository.playbackContextUri.collectAsState()
    val playingState = PlayerRepository.playing.collectAsState()
    val playable = PlayerRepository.playable.collectAsState().derived { it == true }.value

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
            if (playing.value) PlayerRepository.pause() else PlayerRepository.play(context = context)
        },
    ) {
        CachedIcon(
            name = if (playing.value) "pause-circle-outline" else "play-circle-outline",
            size = size,
            contentDescription = "Play",
            tint = LocalColors.current.highlighted(highlight = matchesContext.value),
        )
    }
}
