package com.dzirbel.kotify.ui.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Applies [semantics] for an image with the given [contentDescription].
 *
 * This is typically done internally by Compose when using [androidx.compose.foundation.Image] or
 * [androidx.compose.material.Icon].
 */
fun Modifier.imageSemantics(contentDescription: String?): Modifier {
    return if (contentDescription != null) {
        semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        this
    }
}
