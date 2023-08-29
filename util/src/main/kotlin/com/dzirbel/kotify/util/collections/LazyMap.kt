package com.dzirbel.kotify.util.collections

// TODO document and test
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
