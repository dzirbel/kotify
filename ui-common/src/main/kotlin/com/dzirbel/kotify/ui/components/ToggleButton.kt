package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.util.collections.plusOrMinus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentSet

@Composable
fun ToggleButton(
    toggled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    contentPadding: PaddingValues = PaddingValues(Dimens.space3),
    content: @Composable RowScope.() -> Unit,
) {
    SimpleTextButton(
        modifier = modifier,
        shape = shape,
        selected = toggled,
        onClick = { onToggle(!toggled) },
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun <T> ToggleButtonGroup(
    elements: ImmutableList<T>,
    selectedElements: PersistentSet<T>,
    onSelectElements: (PersistentSet<T>) -> Unit,
    contentPadding: PaddingValues = PaddingValues(Dimens.space3),
    content: @Composable RowScope.(T) -> Unit,
) {
    Surface(elevation = Dimens.componentElevation, shape = RoundedCornerShape(Dimens.cornerSize)) {
        Row(modifier = Modifier.instrument()) {
            for (element in elements) {
                ToggleButton(
                    toggled = selectedElements.contains(element),
                    onToggle = { toggled -> onSelectElements(selectedElements.plusOrMinus(element, toggled)) },
                    contentPadding = contentPadding,
                    content = { content(element) },
                )
            }
        }
    }
}
