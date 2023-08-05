package com.dzirbel.kotify.util

/**
 * Returns a new [Pair] with the first element mapped by [mapper].
 */
fun <A1, A2, B> Pair<A1, B>.mapFirst(mapper: (A1) -> A2): Pair<A2, B> {
    return Pair(mapper(first), second)
}
