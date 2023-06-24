package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.instrumentation.instrument

@Composable
fun PlayButton(context: Player.PlayContext?, size: Dp = Dimens.iconMedium) {
    val matchesContext = Player.playbackContext.value?.uri == context?.contextUri
    val playing = matchesContext && Player.isPlaying.value
    IconButton(
        enabled = Player.playable && context != null,
        modifier = Modifier.instrument().size(size),
        onClick = {
            if (playing) Player.pause() else Player.play(context = context)
        },
    ) {
        CachedIcon(
            name = if (playing) "pause-circle-outline" else "play-circle-outline",
            size = size,
            contentDescription = "Play",
            tint = LocalColors.current.highlighted(highlight = matchesContext),
        )
    }
}
