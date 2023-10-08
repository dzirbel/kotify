package com.dzirbel.kotify.util.collections

/**
 * Returns an [Iterable] which lazily maps the elements of this [Iterable] using the given [mapper].
 *
 * This can be used to avoid allocating a [List] in cases when the list will be immediately discarded. Note that the
 * map is re-applied each time the returned [Iterable] is iterated.
 */
fun <T, R> Iterable<T>.mapLazy(mapper: (T) -> R): Iterable<R> {
    return Iterable {
        val base = iterator()
        @Suppress("IteratorNotThrowingNoSuchElementException")
        object : Iterator<R> {
            override fun hasNext() = base.hasNext()
            override fun next() = mapper(base.next())
        }
    }
}
