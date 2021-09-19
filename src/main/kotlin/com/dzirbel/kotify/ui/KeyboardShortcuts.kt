package com.dzirbel.kotify.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Handler for global keyboard shortcuts.
 */
object KeyboardShortcuts {
    var debugShown by mutableStateOf(false)
        private set

    // TODO these are invoked even when focusing a text input field
    @ExperimentalComposeUiApi
    fun handle(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyUp) return false

        when (event.key) {
            Key.F12 -> debugShown = !debugShown
            Key.Spacebar -> Player.togglePlayback()
            else -> return false
        }

        return true
    }
}
