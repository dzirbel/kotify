package com.dzirbel.kotify.ui.util.instrumentation

import kotlin.reflect.KProperty

/**
 * A simple mutable wrapper around a single [value].
 *
 * This allows keeping a mutable reference to primitive types, which can be remember{}ed, but without causing
 * recomposition on changes (as for State). This is generally only needed for debugging internals of recomposition.
 *
 * [androidx.compose.ui.node.Ref] is similar but does not provide a convenient constructor or getter/setters for
 * delegation.
 */
@Suppress("UseDataClass")
class Ref<T>(var value: T)

operator fun <T> Ref<T>.getValue(thisRef: Any?, property: KProperty<*>): T = this.value

operator fun <T> Ref<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
}
