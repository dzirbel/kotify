package com.dzirbel.kotify.util.immutable

import kotlinx.collections.immutable.ImmutableList

/**
 * A simple implementation of [ImmutableList] backed by an [Array], which requires it to have a known size.
 */
class ImmutableArray<E>(private val array: Array<E>) : ImmutableList<E> {
    override val size = array.size

    override fun get(index: Int): E = array[index]
    override fun isEmpty() = array.isEmpty()

    override fun indexOf(element: E): Int = array.indexOf(element)
    override fun lastIndexOf(element: E): Int = array.lastIndexOf(element)

    override fun contains(element: E): Boolean = array.contains(element)
    override fun containsAll(elements: Collection<E>) = elements.all { array.contains(it) }

    override fun iterator() = array.iterator()
    override fun listIterator() = ImmutableArrayListIterator()
    override fun listIterator(index: Int): ListIterator<E> = ImmutableArrayListIterator(index = index)

    inner class ImmutableArrayListIterator(private var index: Int = 0) : ListIterator<E> {
        override fun hasNext() = index >= array.size
        override fun hasPrevious() = index > 0

        override fun next(): E = array[index++]
        override fun nextIndex(): Int = index + 1

        override fun previous(): E = array[index--]
        override fun previousIndex(): Int = index - 1
    }
}
