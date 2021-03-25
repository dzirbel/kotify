package com.dominiczirbel.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import kotlin.math.roundToInt

private val TRACK_PROGRESS_WIDTH = 1_000.dp
private val TRACK_PROGRESS_HEIGHT = 4.dp
private val ROUNDED_CORNER_SHAPE = RoundedCornerShape(TRACK_PROGRESS_HEIGHT / 2)
private val SEEK_TARGET_SIZE = 12.dp

/**
 * A horizontal slider which displays a [progress] state and can be seeked by dragging or clicking.
 *
 * @param progress the current progress in the slider, or null to disable the slider
 * @param dragKey an optional key which maintains the state of dragging in the slider; when this value changes the drag
 *  state will be reset
 * @param leftContent optional content placed to the left of the slider
 * @param rightContent optional content placed to the right of the slider
 * @param onSeek invoked when the user seeks, either by clicking or dragging, with the seeked location as a percentage
 *  of the maximum slider width, between 0 and 1
 *
 * TODO sometimes jumps for a moment after seeking
 */
@Composable
@Suppress("UnnecessaryParentheses")
fun SeekableSlider(
    progress: Float?,
    dragKey: Any? = null,
    leftContent: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null,
    onSeek: (seekPercent: Float) -> Unit = { }
) {
    progress?.let { require(it in 0f..1f) }

    // whether the user is hovering over the slider
    val hoverState = remember { mutableStateOf(false) }

    // the current x coordinate of the user's pointer, relative to the slider bar, in pixels
    val hoverLocation = remember { mutableStateOf(0f) }

    // the current drag offset, in pixels
    val drag = remember(dragKey) { mutableStateOf(0f) }

    // the final progress amount, accounting for drag, as a percentage between 0 and 1
    val progressOverride = remember(dragKey) { mutableStateOf(progress) }

    // the total width of the slider bar, which is necessary for computing the click location as a percentage of the
    // max width
    val barWidth = remember { mutableStateOf(0) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        leftContent?.let {
            it()
            Spacer(Modifier.width(Dimens.space2))
        }

        val padding = Dimens.space3
        val paddingPx = with(LocalDensity.current) { padding.toPx() }
        Box(
            Modifier
                .pointerMoveFilter(
                    onEnter = { true.also { hoverState.value = true } },
                    onExit = { true.also { hoverState.value = false } },
                    onMove = {
                        // manually adjust for padding
                        hoverLocation.value = it.x - paddingPx
                        true
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val maxWidth = barWidth.value
                    val adjustedX = hoverLocation.value.coerceAtLeast(0f).coerceAtMost(maxWidth.toFloat())
                    val seekPercent = adjustedX / maxWidth
                    drag.value = adjustedX - (progressOverride.value!! * maxWidth)
                    onSeek(seekPercent)
                }
                .padding(padding)
                .size(width = TRACK_PROGRESS_WIDTH, height = TRACK_PROGRESS_HEIGHT)
        ) {
            // the background of the slider bar, representing the maximum progress
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(ROUNDED_CORNER_SHAPE)
                    .background(Colors.current.surface1)
            )

            if (progress != null) {
                Layout(
                    content = {
                        // the foreground of the slider bar, representing the current progress
                        Box(
                            Modifier
                                .fillMaxWidth(fraction = progress)
                                .clip(ROUNDED_CORNER_SHAPE)
                                .background(if (hoverState.value) MaterialTheme.colors.primary else Colors.current.text)
                        )

                        if (hoverState.value) {
                            // a draggable circle for seeking
                            Box(
                                Modifier
                                    .size(SEEK_TARGET_SIZE)
                                    .clip(CircleShape)
                                    .background(Colors.current.text)
                                    .draggable(
                                        state = rememberDraggableState {
                                            drag.value += it
                                        },
                                        orientation = Orientation.Horizontal,
                                        startDragImmediately = true,
                                        onDragStopped = { progressOverride.value?.let(onSeek) }
                                    )
                            )
                        }
                    },
                    measurePolicy = { measurables: List<Measurable>, constraints: Constraints ->
                        require(measurables.size in 1..2)

                        val height = constraints.maxHeight
                        val width = constraints.maxWidth
                        barWidth.value = width

                        // width of the progress bar in pixels, including drag
                        val progressWidth = ((width * progress) + drag.value)
                            .roundToInt()
                            .coerceAtLeast(0)
                            .coerceAtMost(width)

                        // width of the progress bar as a percentage of the total width, between 0 and 1
                        val finalProgress = progressWidth.toFloat() / constraints.maxWidth
                        progressOverride.value = finalProgress

                        val progressBar = measurables[0].measure(
                            Constraints.fixed(width = progressWidth, height = height)
                        )
                        val seekTarget = measurables.getOrNull(1)?.measure(Constraints())

                        layout(width, height) {
                            progressBar.place(0, 0)

                            seekTarget?.let {
                                check(it.width == it.height)
                                val size = it.width

                                it.place(
                                    x = progressBar.width - (size / 2),
                                    y = (height / 2) - (size / 2)
                                )
                            }
                        }
                    }
                )
            }
        }

        rightContent?.let {
            Spacer(Modifier.width(Dimens.space2))
            it()
        }
    }
}
