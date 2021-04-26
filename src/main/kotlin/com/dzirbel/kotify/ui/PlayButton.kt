package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens

@Composable
fun PlayButton(contextUri: String, size: Dp = Dimens.iconMedium) {
    val matchesContext = Player.playbackContext.value?.uri == contextUri
    val playing = Player.isPlaying.value && matchesContext
    IconButton(
        enabled = Player.playable,
        modifier = Modifier.size(size),
        onClick = {
            if (playing) Player.pause() else Player.play(contextUri = contextUri)
        }
    ) {
        CachedIcon(
            name = if (playing) "pause-circle-outline" else "play-circle-outline",
            size = size,
            contentDescription = "Play",
            tint = Colors.current.highlighted(highlight = matchesContext)
        )
    }
}
