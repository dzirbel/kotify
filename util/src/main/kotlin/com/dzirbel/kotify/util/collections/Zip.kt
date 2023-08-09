package com.dzirbel.kotify.util.collections

/**
 * Invokes [onEach] for each pair built from the elements of this [Iterable] and [other] with the same index, up to the
 * minimum index of the two collections.
 *
 * Like [Iterable.zip] but doesn't construct a list of the resulting zipped values.
 */
inline fun <A, B> Iterable<A>.zipEach(other: Iterable<B>, onEach: (A, B) -> Unit) {
    val first = iterator()
    val second = other.iterator()
    while (first.hasNext() && second.hasNext()) {
        onEach(first.next(), second.next())
    }
}

/**
 * Returns a lazily-generator [Iterable] of the pair of this [Iterable] and [other], up to the minimum of the two
 * iterable lengths.
 */
fun <A, B> Iterable<A>.zipLazy(other: Iterable<B>): Iterable<Pair<A, B>> {
    return object : Iterable<Pair<A, B>> {
        override fun iterator(): Iterator<Pair<A, B>> {
            @Suppress("IteratorNotThrowingNoSuchElementException") // thrown by delegate iterators
            return object : Iterator<Pair<A, B>> {
                val iteratorA = this@zipLazy.iterator()
                val iteratorB = other.iterator()

                override fun hasNext() = iteratorA.hasNext() && iteratorB.hasNext()
                override fun next() = Pair(iteratorA.next(), iteratorB.next())
            }
        }
    }
}
