package com.dzirbel.kotify.util

import kotlin.properties.ReadOnlyProperty

/**
 * Returns a [ReadOnlyProperty] which delegates to this one, mapping its values via [map].
 */
fun <T, V, W> ReadOnlyProperty<T, V>.mapped(map: (V) -> W): ReadOnlyProperty<T, W> {
    val base = this
    return ReadOnlyProperty { thisRef, property -> map(base.getValue(thisRef, property)) }
}
