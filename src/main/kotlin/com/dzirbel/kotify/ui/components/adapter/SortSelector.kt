package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.util.minusAt

@Composable
fun <T> SortSelector(
    allowEmpty: Boolean = false,
    sortProperties: List<SortableProperty<T>>,
    sorts: List<Sort<T>>,
    onSetSort: (List<Sort<T>>) -> Unit,
) {
    LocalColors.current.withSurface {
        Row(
            modifier = Modifier.surfaceBackground(RoundedCornerShape(size = Dimens.cornerSize)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (sorts.isEmpty()) {
                CachedIcon(
                    name = "sort",
                    size = Dimens.iconSmall,
                    modifier = Modifier.padding(horizontal = Dimens.space2),
                )
            }

            sorts.forEachIndexed { index, sort ->
                Row {
                    val changeDropdownExpanded = remember { mutableStateOf(false) }
                    SortSelectorButton(
                        contentPadding = PaddingValues(horizontal = Dimens.space3),
                        onClick = { changeDropdownExpanded.value = true },
                    ) {
                        if (index == 0) {
                            CachedIcon(
                                name = "sort",
                                size = Dimens.iconSmall,
                                modifier = Modifier.padding(end = Dimens.space2),
                            )
                        }

                        Text(sort.sortableProperty.sortTitle)

                        SortPickerDropdown(
                            expanded = changeDropdownExpanded.value,
                            sortProperties = { sortProperties.minus(sort.sortableProperty) },
                            onDismissRequest = { changeDropdownExpanded.value = false },
                            onPickSort = { property ->
                                val newSort = Sort(sortableProperty = property, sortOrder = property.defaultOrder)
                                onSetSort(
                                    sorts
                                        .toMutableList()
                                        // replace the sort at this index with the new property
                                        .apply { set(index, newSort) }
                                        // remove any other (i.e. not at this index) sorts which also use this property
                                        .filterIndexed { i, s -> i == index || s.sortableProperty != property }
                                )
                            }
                        )
                    }

                    SortSelectorButton(
                        onClick = {
                            // flip the sort at this index
                            val flipped = sort.copy(sortOrder = sort.sortOrder.flipped)
                            onSetSort(sorts.toMutableList().apply { set(index, flipped) })
                        },
                    ) {
                        Icon(
                            imageVector = sort.sortOrder.icon,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconSmall),
                            tint = LocalColors.current.primary,
                        )
                    }

                    if (allowEmpty || sorts.size > 1) {
                        SortSelectorButton(onClick = { onSetSort(sorts.minusAt(index)) }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.iconSmall),
                            )
                        }
                    }
                }

                if (index != sorts.lastIndex) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = Dimens.space2).size(Dimens.iconSmall),
                    )
                }
            }

            if (sortProperties.size > sorts.size && sorts.lastOrNull()?.sortableProperty?.terminal != true) {
                val addDropdownExpanded = remember { mutableStateOf(false) }
                SortSelectorButton(onClick = { addDropdownExpanded.value = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall),
                    )

                    SortPickerDropdown(
                        expanded = addDropdownExpanded.value,
                        sortProperties = {
                            val selectedSortProperties = sorts.mapTo(mutableSetOf()) { it.sortableProperty }
                            sortProperties.minus(selectedSortProperties)
                        },
                        onDismissRequest = { addDropdownExpanded.value = false },
                        onPickSort = { property ->
                            val newSort = Sort(sortableProperty = property, sortOrder = property.defaultOrder)
                            onSetSort(sorts.plus(newSort))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortSelectorButton(
    contentPadding: PaddingValues = PaddingValues(all = Dimens.space2),
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    SimpleTextButton(
        onClick = onClick,
        contentPadding = contentPadding,
        enforceMinWidth = false,
        enforceMinHeight = true,
        content = content,
    )
}

@Composable
private fun <T> SortPickerDropdown(
    expanded: Boolean,
    sortProperties: () -> List<SortableProperty<T>>,
    onDismissRequest: () -> Unit,
    onPickSort: (SortableProperty<T>) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        sortProperties().forEach { property ->
            DropdownMenuItem(
                onClick = {
                    onDismissRequest()
                    onPickSort(property)
                }
            ) {
                Text(property.sortTitle)
            }
        }
    }
}
