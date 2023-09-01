package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.material.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A [Checkbox] wrapped in a clickable [Row] with the given [label] as a label at the end.
 */
@Composable
fun CheckboxWithLabel(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clickable { onCheckedChange(!checked) }
            .padding(top = Dimens.space1, bottom = Dimens.space1, end = Dimens.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)

        HorizontalSpacer(Dimens.space2)

        label()
    }
}

/**
 * A [TriStateCheckbox] wrapped in a clickable [Row] with the given [label] as a label at the end.
 */
@Composable
fun TriStateCheckboxWithLabel(
    state: ToggleableState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(top = Dimens.space1, bottom = Dimens.space1, end = Dimens.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TriStateCheckbox(state = state, onClick = null)

        HorizontalSpacer(Dimens.space2)

        label()
    }
}
