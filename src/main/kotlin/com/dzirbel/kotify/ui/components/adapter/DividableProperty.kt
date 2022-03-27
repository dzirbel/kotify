package com.dzirbel.kotify.ui.components.adapter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.theme.Dimens

/**
 * Represents a property by which objects of type [E] can be divided into groups.
 *
 * Divisions may be any object, possibly null, as determined by [divisionFor] for each element. A [DividableProperty]
 * must provide a way to order divisions based on [compareDivisions] and content to be rendered as the header of a
 * division in [divisionHeader].
 */
interface DividableProperty<E> : AdapterProperty<E> {
    /**
     * A user-readable name of this property, specific to its use in dividing. By default uses [title] but may be
     * overridden to use a specific name for dividing.
     */
    val dividerTitle: String
        get() = title

    /**
     * The default [SortOrder] in which orderings of divisions should be oriented.
     */
    val defaultDivisionSortOrder: SortOrder
        get() = SortOrder.ASCENDING

    /**
     * Determines the division to which [element] belongs.
     */
    fun divisionFor(element: E): Any?

    /**
     * Compares divisions [first] and [second] relative to [sortOrder], returning a positive integer if [first] is
     * greater than [second], a negative integer if [first] is less than [second], or zero if they are equal.
     *
     * Note that [sortOrder] must be provided here in order to allow comparisons which are not invertable, e.g.
     * comparisons which put null values last whether in ascending or descending order.
     */
    fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int

    /**
     * Renders [division] as a string; used to render its [divisionHeader].
     */
    fun divisionTitle(division: Any?): String? = division?.toString().orEmpty()

    /**
     * Renders a display of the header of the given [division], using its [divisionTitle].
     */
    @Composable
    fun divisionHeader(division: Any?) {
        divisionTitle(division)?.let { divisionTitle ->
            Box {
                Text(
                    text = divisionTitle,
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(horizontal = Dimens.space5, vertical = Dimens.space2),
                )

                HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
