package com.dzirbel.kotify.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.repository.player.Player

/**
 * Handler for global keyboard shortcuts.
 */
class KeyboardShortcuts(private val player: Player) {
    fun handle(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyUp) return false

        when (event.key) {
            Key.F12 -> Settings.debugPanelOpen = !Settings.debugPanelOpen
            Key.Spacebar ->
                player.playing.value?.let { playing ->
                    if (playing.value) player.pause() else player.play()
                }
            else -> return false
        }

        return true
    }
}
