package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.Divider
import com.dzirbel.kotify.ui.components.adapter.flipped
import com.dzirbel.kotify.ui.components.adapter.icon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.util.applyIf
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import kotlinx.collections.immutable.ImmutableList

/**
 * Renders a standard selector among the given [dividableProperties], displaying [currentDivider] as the currently
 * selected one and invoking [onSelectDivider] when the user chooses a new [Divider] (or null to remove divisions).
 */
@Composable
fun <E> DividerSelector(
    dividableProperties: ImmutableList<DividableProperty<E>>,
    currentDivider: Divider<E>?,
    contentPadding: PaddingValues = PaddingValues(Dimens.space3),
    onSelectDivider: (Divider<E>?) -> Unit,
) {
    Surface(
        modifier = Modifier.instrument(),
        elevation = Dimens.componentElevation,
        shape = RoundedCornerShape(size = Dimens.cornerSize),
    ) {
        FlowRow(
            modifier = Modifier.applyIf(currentDivider != null) { background(KotifyColors.current.selectedBackground) },
            verticalArrangement = Arrangement.Center,
        ) {
            val dropdownExpanded = remember { mutableStateOf(false) }
            SimpleTextButton(
                onClick = {
                    dropdownExpanded.value = true
                },
                contentPadding = contentPadding,
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
                        Text(text = "Group by...", fontStyle = FontStyle.Italic, maxLines = 1)
                    }
                } else {
                    Text(currentDivider.dividableProperty.dividerTitle, maxLines = 1)
                }

                DropdownMenu(expanded = dropdownExpanded.value, onDismissRequest = { dropdownExpanded.value = false }) {
                    dividableProperties.forEach { dividableProperty ->
                        if (dividableProperty.dividerTitle != currentDivider?.dividableProperty?.dividerTitle) {
                            DropdownMenuItem(
                                onClick = {
                                    dropdownExpanded.value = false

                                    onSelectDivider(Divider(dividableProperty))
                                },
                            ) {
                                Text(dividableProperty.dividerTitle, maxLines = 1)
                            }
                        }
                    }
                }
            }

            if (currentDivider != null) {
                SimpleTextButton(
                    onClick = {
                        onSelectDivider(
                            currentDivider.copy(divisionSortOrder = currentDivider.divisionSortOrder.flipped),
                        )
                    },
                    contentPadding = contentPadding,
                    enforceMinWidth = false,
                    enforceMinHeight = true,
                ) {
                    Icon(
                        imageVector = currentDivider.divisionSortOrder.icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall),
                    )
                }

                SimpleTextButton(
                    onClick = { onSelectDivider(null) },
                    contentPadding = contentPadding,
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
