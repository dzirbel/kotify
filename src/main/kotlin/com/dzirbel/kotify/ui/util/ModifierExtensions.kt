package com.dzirbel.kotify.ui.util

import androidx.compose.ui.Modifier

/**
 * Convenience function applying [statement] to this [Modifier] if [condition] is true.
 *
 * Inline to allow composable modifiers.
 */
inline fun Modifier.applyIf(condition: Boolean, statement: Modifier.() -> Modifier): Modifier {
    return if (condition) this.statement() else this
}
