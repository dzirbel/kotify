package com.dominiczirbel.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.dominiczirbel.ui.TRACK_PROGRESS_HEIGHT
import com.dominiczirbel.ui.TRACK_PROGRESS_WIDTH
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens

// TODO add seeking
@Composable
fun SeekableSlider(
    progress: Float?,
    leftContent: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null
) {
    progress?.let { require(it in 0f..1f) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        leftContent?.let {
            it()
            Spacer(Modifier.width(Dimens.space3))
        }

        val shape = RoundedCornerShape(TRACK_PROGRESS_HEIGHT / 2)
        Box(
            Modifier
                .size(width = TRACK_PROGRESS_WIDTH, height = TRACK_PROGRESS_HEIGHT)
                .clip(shape)
                .background(Colors.current.surface1)
        ) {
            if (progress != null) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction = progress)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(Colors.current.text)
                )
            }
        }

        rightContent?.let {
            Spacer(Modifier.width(Dimens.space3))
            it()
        }
    }
}
