package com.dzirbel.kotify.ui.components.adapter

import com.dzirbel.kotify.ui.components.table.Column

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
fun <E> List<AdapterProperty<E>>.sortableProperties(): List<SortableProperty<E>> {
    return filterIsInstance<SortableProperty<E>>()
}

/**
 * Filters this [List] for only the [DividableProperty]s it contains.
 */
fun <E> List<AdapterProperty<E>>.dividableProperties(): List<DividableProperty<E>> {
    return filterIsInstance<DividableProperty<E>>()
}

/**
 * Filters this [List] for only the [Column]s it contains.
 */
fun <E> List<AdapterProperty<E>>.columns(): List<Column<E>> {
    return filterIsInstance<Column<E>>()
}
