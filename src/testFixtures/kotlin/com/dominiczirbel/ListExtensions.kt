package com.dominiczirbel

import com.google.common.truth.Truth.assertThat

/**
 * [zip]s this [List] with [other], pairing values one-to-one when [matcher] is true.
 *
 * This [List] and [other] must have the same size, and there must be a one-to-one association between the two lists
 * given by [matcher]; that is:
 * - for each element A of this [List]
 * - there must exist a unique element B of [other]
 * - for which matcher(A, B) is true; matcher(A, C) for all other elements C of [other] must be false
 *
 * This is typically used to join two lists by unique ID, where matcher checks for ID equality.
 */
fun <T, R> List<T>.zipWithBy(other: List<R>, matcher: (T, R) -> Boolean): List<Pair<T, R>> {
    assertThat(this).hasSize(other.size)
    return map { thisElement ->
        val matching = other.filter { matcher(thisElement, it) }
        assertThat(matching).hasSize(1)

        thisElement to matching.first()
    }
}
