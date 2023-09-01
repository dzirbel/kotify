package com.dzirbel.kotify.ui.util

import androidx.compose.ui.state.ToggleableState

/**
 * Returns a [ToggleableState] reflecting whether [selectedValues] contains all, some, or none of the elements in this
 * [Iterable].
 *
 * That is, if none of the values in this iterable are in [selectedValues], [ToggleableState.Off] is returned; if all of
 * them are in this iterable, [ToggleableState.On] is returned; and if some are in this iterable and some are not,
 * [ToggleableState.Indeterminate] is returned. If this [Iterable] is empty, [ToggleableState.On] is returned.
 *
 * Optimized somewhat to short-circuit as soon as possible (as opposed to e.g. counting the number of elements in
 * [selectedValues] that are in this iterable and comparing it to the size).
 */
fun <T> Iterable<T>.groupToggleState(selectedValues: Set<T>): ToggleableState {
    var any = false
    var all = true
    for (element in this) {
        if (element in selectedValues) {
            any = true
            if (!all) return ToggleableState.Indeterminate
        } else {
            all = false
            if (any) return ToggleableState.Indeterminate
        }
    }

    return if (all) ToggleableState.On else ToggleableState.Off
}
