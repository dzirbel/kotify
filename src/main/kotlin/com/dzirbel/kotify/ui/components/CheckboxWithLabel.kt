package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * A [Checkbox] wrapped in a clickable [Row] with the given [label] as a label at the end.
 */
@Composable
fun CheckboxWithLabel(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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
