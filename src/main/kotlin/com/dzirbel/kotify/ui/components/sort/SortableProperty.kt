package com.dzirbel.kotify.ui.components.sort

/**
 * Represents an abstract property of a collection of data (e.g. table or grid) which specifies how its elements of type
 * [E] can be sorted.
 */
abstract class SortableProperty<E>(val sortTitle: String) {
    abstract fun compare(first: IndexedValue<E>, second: IndexedValue<E>): Int
}
