package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.util.collections.plusOrMinus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet

@Composable
fun ToggleButton(
    toggled: Boolean,
    onToggle: (Boolean) -> Unit,
    shape: Shape = RectangleShape,
    content: @Composable RowScope.() -> Unit,
) {
    LocalColors.current.WithSurface(increment = if (toggled) Colors.INCREMENT_LARGE else 0) {
        SimpleTextButton(
            modifier = Modifier
                .border(
                    shape = shape,
                    color = LocalColors.current.dividerColor,
                    width = Dimens.divider,
                )
                .run {
                    if (toggled) surfaceBackground(shape = shape) else this
                },
            shape = shape,
            textColor = if (toggled) LocalColors.current.primary else LocalColors.current.text,
            onClick = { onToggle(!toggled) },
            content = content,
        )
    }
}

@Composable
fun <T> ToggleButtonGroup(
    elements: ImmutableList<T>,
    selectedElements: PersistentSet<T>,
    onSelectElements: (PersistentSet<T>) -> Unit,
    content: @Composable (T) -> Unit,
) {
    Row(
        modifier = Modifier.instrument(),
        horizontalArrangement = Arrangement.spacedBy(-Dimens.divider),
    ) {
        for ((index, element) in elements.withIndex()) {
            val isFirst = index == 0
            val isLast = index == elements.lastIndex

            val shape = if (isFirst || isLast) {
                val start = if (isFirst) Dimens.cornerSize else 0.dp
                val end = if (isLast) Dimens.cornerSize else 0.dp
                RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
            } else {
                RectangleShape
            }

            ToggleButton(
                shape = shape,
                toggled = selectedElements.contains(element),
                onToggle = { toggled ->
                    onSelectElements(selectedElements.plusOrMinus(element, toggled))
                },
                content = { content(element) },
            )
        }
    }
}
