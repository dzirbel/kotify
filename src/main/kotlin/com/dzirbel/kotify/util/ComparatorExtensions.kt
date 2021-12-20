package com.dzirbel.kotify.util

/**
 * Returns a [Comparator] which compares elements of type [T] according to the [Comparator]s in this list, in order.
 * I.e. if the first [Comparator] determines a comparison is less or greater than, that comparison will be returned,
 * otherwise the next [Comparator] will be checked, and so on.
 */
fun <T> Iterable<Comparator<T>>.compareInOrder(): Comparator<T> {
    return reduce { first, second -> first.thenComparing(second) }
}
