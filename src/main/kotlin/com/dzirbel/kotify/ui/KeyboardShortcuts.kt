package com.dzirbel.kotify.ui

import androidx.compose.desktop.AppWindow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key

/**
 * Handler for global keyboard shortcuts.
 */
object KeyboardShortcuts {
    var debugShown by mutableStateOf(false)
        private set

    // TODO these are invoked even when focusing a text input field (but modifier-based capture doesn't seem to work at
    //  all)
    fun register(window: AppWindow) {
        window.keyboard.apply {
            setShortcut(Key.F12) {
                debugShown = !debugShown
            }

            setShortcut(Key.Spacebar) {
                Player.togglePlayback()
            }
        }
    }
}
