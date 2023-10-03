package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.instrumentation.Ref
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import kotlin.math.roundToInt

val DEFAULT_SLIDER_HEIGHT = 4.dp
val DEFAULT_SEEK_TARGET_SIZE = 12.dp

/**
 * A horizontal slider which displays a [progress] state and can be seeked by dragging or clicking.
 *
 * @param progress the current progress in the slider, or null to disable the slider
 * @param sliderWidth the width of the slider
 * @param sliderHeight the height of the slider, by default [DEFAULT_SLIDER_HEIGHT]
 * @param seekTargetSize the size of the seek touch-target, by default [DEFAULT_SEEK_TARGET_SIZE]
 * @param leftLabel optional content placed to the left of the slider
 * @param rightLabel optional content placed to the right of the slider
 * @param onSeek invoked when the user seeks, either by clicking or dragging, with the seeked location as a percentage
 *  of the maximum slider width, between 0 and 1
 */
@Composable
fun SeekableSlider(
    progress: () -> Float?,
    sliderWidth: Dp = Dp.Unspecified,
    sliderHeight: Dp = DEFAULT_SLIDER_HEIGHT,
    seekTargetSize: Dp = DEFAULT_SEEK_TARGET_SIZE,
    leftLabel: (@Composable () -> Unit)? = null,
    rightLabel: (@Composable () -> Unit)? = null,
    onSeek: (seekPercent: Float) -> Unit = { },
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val hovering = hoverInteractionSource.collectIsHoveredAsState().value

    // the current x coordinate of the user's pointer, relative to the slider bar, in pixels
    val hoverLocation = remember { Ref(0f) }

    // the current drag offset, in pixels
    val drag = remember { mutableStateOf(0f) }

    // the total width of the slider bar, which is necessary for computing the click location as a percentage of the
    // max width
    val barWidth = remember { Ref(0) }

    // seeks to the current hover location
    fun seek() {
        val maxWidth = barWidth.value.takeIf { it != 0 } ?: return
        val adjustedX = hoverLocation.value.coerceAtLeast(0f).coerceAtMost(maxWidth.toFloat())
        onSeek(adjustedX / maxWidth)
    }

    Row(
        modifier = Modifier.instrument(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
    ) {
        leftLabel?.invoke()

        val padding = Dimens.space3
        val paddingPx = with(LocalDensity.current) { padding.toPx() }
        Box(
            Modifier
                .hoverable(hoverInteractionSource)
                // use onPointerEvent to get raw pointer data; pointerMoveFilter does not register moves when the cursor
                // is being dragged
                .onPointerEvent(PointerEventType.Move) { event ->
                    event.changes.firstOrNull()?.let {
                        // manually adjust for padding
                        hoverLocation.value = it.position.x - paddingPx
                    }
                }
                .onClick { seek() }
                .padding(padding)
                .size(width = sliderWidth, height = sliderHeight)
                .let {
                    if (sliderWidth.isSpecified) it else it.weight(1f)
                },
        ) {
            val roundedCornerShape = RoundedCornerShape(sliderHeight / 2)

            // the background of the slider bar, representing the maximum progress
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LocalContentColor.current.copy(alpha = ContentAlpha.disabled), roundedCornerShape),
            )

            Layout(
                content = {
                    // the foreground of the slider bar, representing the current progress
                    Box(
                        Modifier
                            .clip(roundedCornerShape)
                            .background(KotifyColors.highlighted(highlight = hovering)),
                    )

                    if (hovering) {
                        // a draggable circle for seeking
                        Box(
                            Modifier
                                .size(seekTargetSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colors.onBackground)
                                .draggable(
                                    state = rememberDraggableState { drag.value += it },
                                    orientation = Orientation.Horizontal,
                                    startDragImmediately = true,
                                    onDragStopped = { _ ->
                                        drag.value = 0f
                                        seek()
                                    },
                                ),
                        )
                    }
                },
                measurePolicy = { measurables: List<Measurable>, constraints: Constraints ->
                    require(measurables.size in 1..2)

                    val height = constraints.maxHeight
                    val width = constraints.maxWidth
                    barWidth.value = width

                    val progressBar = progress()?.let { currentProgress ->
                        // width of the progress bar in pixels, including drag
                        val progressWidth = ((width * currentProgress) + drag.value)
                            .roundToInt()
                            .coerceAtLeast(0)
                            .coerceAtMost(width)

                        measurables[0].measure(
                            Constraints.fixed(width = progressWidth, height = height),
                        )
                    }

                    val seekTarget = measurables.getOrNull(1)?.measure(Constraints())

                    layout(width, height) {
                        progressBar?.place(0, 0)

                        if (seekTarget != null && progressBar != null) {
                            check(seekTarget.width == seekTarget.height)
                            val size = seekTarget.width

                            seekTarget.place(
                                x = progressBar.width - (size / 2),
                                y = (height / 2) - (size / 2),
                            )
                        }
                    }
                },
            )
        }

        rightLabel?.invoke()
    }
}
