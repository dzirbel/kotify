package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
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
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.flipped
import com.dzirbel.kotify.ui.components.adapter.icon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList

/**
 * Renders a standard selector among the given [sortableProperties], displaying [sorts] as the currently selected
 * ordered list of sorting criteria and invoking [onSetSort] when the user chooses a new list of [Sort]s.
 */
@Composable
fun <T> SortSelector(
    sortableProperties: ImmutableList<SortableProperty<T>>,
    sorts: PersistentList<Sort<T>>,
    allowEmpty: Boolean = false,
    onSetSort: (PersistentList<Sort<T>>) -> Unit,
) {
    Surface(
        modifier = Modifier.instrument(),
        elevation = Dimens.componentElevation,
        shape = RoundedCornerShape(size = Dimens.cornerSize),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        onClick = { changeDropdownExpanded.value = true },
                        contentPadding = PaddingValues(horizontal = Dimens.space3),
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
                            sortProperties = {
                                sortableProperties.filterNot { it.sortTitle == sort.sortableProperty.sortTitle }
                            },
                            onDismissRequest = { changeDropdownExpanded.value = false },
                            onPickSort = { property ->
                                val newSort = Sort(sortableProperty = property, sortOrder = property.defaultSortOrder)
                                onSetSort(
                                    sorts
                                        .toMutableList()
                                        // replace the sort at this index with the new property
                                        .apply { set(index, newSort) }
                                        // remove any other (i.e. not at this index) sorts which also use this property
                                        .filterIndexed { i, s -> i == index || s.sortableProperty != property }
                                        .toPersistentList(),
                                )
                            },
                        )
                    }

                    SortSelectorButton(
                        onClick = {
                            // flip the sort at this index
                            val flipped = sort.copy(sortOrder = sort.sortOrder.flipped)
                            onSetSort(sorts.mutate { it[index] = flipped })
                        },
                    ) {
                        Icon(
                            imageVector = sort.sortOrder.icon,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconSmall),
                            tint = MaterialTheme.colors.primary,
                        )
                    }

                    if (allowEmpty || sorts.size > 1) {
                        SortSelectorButton(onClick = { onSetSort(sorts.mutate { it.removeAt(index) }) }) {
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

            if (sortableProperties.size > sorts.size && sorts.lastOrNull()?.sortableProperty?.terminalSort != true) {
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
                            val selectedSortProperties = sorts.mapTo(mutableSetOf()) { it.sortableProperty.title }
                            sortableProperties.filterNot { it.sortTitle in selectedSortProperties }
                        },
                        onDismissRequest = { addDropdownExpanded.value = false },
                        onPickSort = { property ->
                            val newSort = Sort(sortableProperty = property, sortOrder = property.defaultSortOrder)
                            onSetSort(sorts.mutate { it.plus(newSort) })
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortSelectorButton(
    onClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(all = Dimens.space2),
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
                },
            ) {
                Text(property.sortTitle)
            }
        }
    }
}
