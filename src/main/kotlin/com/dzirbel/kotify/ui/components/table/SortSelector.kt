package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors

@Composable
fun <T> SortSelector(
    columns: List<Column<T>>,
    sorts: List<Sort<T>>,
    onSetSort: (List<Sort<T>>) -> Unit,
) {
    Row(
        modifier = Modifier.padding(Dimens.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space3),
    ) {
        sorts.forEachIndexed { index, sort ->
            Row(
                modifier = Modifier.background(
                    color = LocalColors.current.surface2,
                    shape = RoundedCornerShape(size = Dimens.cornerSize)
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space1),
            ) {
                SimpleTextButton(
                    onClick = {
                        // flip the sort at this index
                        val flipped = sort.copy(
                            sortOrder = when (sort.sortOrder) {
                                SortOrder.ASCENDING -> SortOrder.DESCENDING
                                SortOrder.DESCENDING -> SortOrder.ASCENDING
                            }
                        )

                        onSetSort(sorts.toMutableList().apply { set(index, flipped) })
                    }
                ) {
                    Text(sort.column.name)

                    Icon(
                        imageVector = sort.sortOrder.icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall),
                        tint = LocalColors.current.primary,
                    )
                }

                SimpleTextButton(
                    onClick = { onSetSort(sorts.minus(sort)) },
                    contentPadding = PaddingValues(all = Dimens.space2),
                    enforceMinWidth = false,
                    enforceMinHeight = true,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconTiny),
                    )
                }
            }

            if (index != sorts.lastIndex) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconTiny),
                )
            }
        }

        val addDropdownExpanded = remember { mutableStateOf(false) }

        val sortableColumns = remember(sorts, columns) {
            val sortColumns = sorts.mapTo(mutableSetOf()) { it.column }
            columns.filter { it.sortable && it !in sortColumns }
        }

        IconButton(
            enabled = sortableColumns.isNotEmpty(),
            onClick = { addDropdownExpanded.value = true },
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSmall),
            )
        }

        DropdownMenu(
            expanded = addDropdownExpanded.value,
            onDismissRequest = { addDropdownExpanded.value = false }
        ) {
            sortableColumns.forEach { column ->
                DropdownMenuItem(
                    onClick = {
                        addDropdownExpanded.value = false

                        val newSort = Sort(column = column, sortOrder = SortOrder.ASCENDING)
                        onSetSort(sorts.plus(newSort))
                    }
                ) {
                    Text(column.name)
                }
            }
        }
    }
}
