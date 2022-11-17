package com.dzirbel.kotify.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent

/**
 * Workaround which consumes all key events and prevents them from propagating to parent elements.
 *
 * Should be applied to evey TextField element per https://github.com/JetBrains/compose-jb/issues/1925.
 */
fun Modifier.consumeKeyEvents() = onKeyEvent { true }

/**
 * Convenience function applying [statement] to this [Modifier] if [condition] is true.
 *
 * Inline to allow composable modifiers.
 */
inline fun Modifier.applyIf(condition: Boolean, statement: Modifier.() -> Modifier): Modifier {
    return if (condition) this.statement() else this
}
