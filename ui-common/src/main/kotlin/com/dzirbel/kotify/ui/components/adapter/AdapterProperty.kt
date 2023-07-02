package com.dzirbel.kotify.ui.components.adapter

import com.dzirbel.kotify.ui.components.table.Column
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Represents a property of an object of type [E]. This is a marker interface for a number of functional properties such
 * as [SortableProperty] which represents a property by which objects of type [E] may be sorted.
 */
interface AdapterProperty<E> {
    /**
     * The user-readable name of this property.
     */
    val title: String
}

/**
 * Filters this [List] for only the [SortableProperty]s it contains.
 */
fun <E> List<AdapterProperty<E>>.sortableProperties(): ImmutableList<SortableProperty<E>> {
    return filterIsInstance<SortableProperty<E>>().toImmutableList()
}

/**
 * Filters this [List] for only the [DividableProperty]s it contains.
 */
fun <E> List<AdapterProperty<E>>.dividableProperties(): ImmutableList<DividableProperty<E>> {
    return filterIsInstance<DividableProperty<E>>().toImmutableList()
}

/**
 * Filters this [List] for only the [Column]s it contains.
 */
fun <E> List<AdapterProperty<E>>.columns(): List<Column<E>> {
    return filterIsInstance<Column<E>>()
}
