package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground

@Composable
fun <E> DividerSelector(
    dividers: List<Divider<E>>,
    currentDivider: Divider<E>?,
    currentDividerSortOrder: SortOrder?,
    onSelectDivider: (Divider<E>?, SortOrder?) -> Unit,
) {
    LocalColors.current.withSurface {
        Row(
            modifier = Modifier.surfaceBackground(RoundedCornerShape(size = Dimens.cornerSize)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dropdownExpanded = remember { mutableStateOf(false) }
            SimpleTextButton(
                onClick = {
                    dropdownExpanded.value = true
                },
                contentPadding = PaddingValues(all = Dimens.space2),
                enforceMinWidth = false,
                enforceMinHeight = true,
            ) {
                CachedIcon(
                    name = "horizontal-split",
                    size = Dimens.iconSmall,
                    modifier = Modifier.padding(end = Dimens.space2),
                )

                if (currentDivider == null) {
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            text = "Group by...",
                            fontStyle = FontStyle.Italic,
                        )
                    }
                } else {
                    Text(currentDivider.dividerTitle)
                }

                DropdownMenu(expanded = dropdownExpanded.value, onDismissRequest = { dropdownExpanded.value = false }) {
                    dividers.forEach { divider ->
                        if (divider.dividerTitle != currentDivider?.dividerTitle) {
                            DropdownMenuItem(
                                onClick = {
                                    dropdownExpanded.value = false

                                    onSelectDivider(divider, divider.defaultDivisionSortOrder)
                                }
                            ) {
                                Text(divider.dividerTitle)
                            }
                        }
                    }
                }
            }

            if (currentDivider != null) {
                SimpleTextButton(
                    onClick = {
                        onSelectDivider(currentDivider, currentDividerSortOrder?.flipped)
                    },
                    contentPadding = PaddingValues(all = Dimens.space2),
                    enforceMinWidth = false,
                    enforceMinHeight = true,
                ) {
                    Icon(
                        imageVector = currentDividerSortOrder.icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall),
                        tint = LocalColors.current.primary,
                    )
                }

                SimpleTextButton(
                    onClick = { onSelectDivider(null, null) },
                    contentPadding = PaddingValues(all = Dimens.space2),
                    enforceMinWidth = false,
                    enforceMinHeight = true,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall),
                    )
                }
            }
        }
    }
}
