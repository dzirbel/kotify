package com.dzirbel.kotify.ui.framework

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable

/**
 * Simple wrapper to render this [Page] via [Page.bind].
 */
@Composable
fun Page<*>.render() {
    Box {
        with(this@render) {
            bind(visible = true)
        }
    }
}
