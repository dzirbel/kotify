package com.dzirbel.kotify.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.repository.Player

/**
 * Handler for global keyboard shortcuts.
 */
object KeyboardShortcuts {
    fun handle(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyUp) return false

        when (event.key) {
            Key.F12 -> Settings.debugPanelOpen = !Settings.debugPanelOpen
            Key.Spacebar -> Player.togglePlayback()
            else -> return false
        }

        return true
    }
}
