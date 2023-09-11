package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.ui.theme.KotifyColors

/**
 * Specifies how [Log.Event] items are rendered in [LogList]; functions may be overridden to customize the display.
 */
interface LogEventDisplay<T> {
    /**
     * Returns the [Color] to use in icons for events with the given [Log.Event.Type].
     */
    val Log.Event.Type.iconColor: Color
        @Composable
        get() {
            return when (this) {
                Log.Event.Type.INFO -> MaterialTheme.colors.onBackground
                Log.Event.Type.SUCCESS -> KotifyColors.current.success
                Log.Event.Type.WARNING -> KotifyColors.current.warning
                Log.Event.Type.ERROR -> MaterialTheme.colors.error
            }
        }

    /**
     * Returns the title string to display for the given [event], from the given [log]; by default the
     * [Log.Event.title].
     */
    fun title(log: Log<T>, event: Log.Event<T>): String = event.title

    /**
     * Returns the content string to display for the given [event]; by default the [Log.Event.content].
     */
    fun content(event: Log.Event<T>): String? = event.content

    /**
     * Renders the icon for the given [event].
     */
    @Composable
    fun Icon(event: Log.Event<T>, modifier: Modifier) {
        Icon(
            imageVector = when (event.type) {
                Log.Event.Type.INFO -> Icons.Default.Info
                Log.Event.Type.SUCCESS -> Icons.Default.Check
                Log.Event.Type.WARNING -> Icons.Default.Warning
                Log.Event.Type.ERROR -> Icons.Default.Close
            },
            contentDescription = null,
            modifier = modifier,
            tint = event.type.iconColor,
        )
    }
}
