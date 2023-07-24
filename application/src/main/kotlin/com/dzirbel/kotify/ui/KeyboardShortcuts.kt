package com.dzirbel.kotify.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.repository.player.PlayerRepository

/**
 * Handler for global keyboard shortcuts.
 */
object KeyboardShortcuts {
    fun handle(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyUp) return false

        when (event.key) {
            Key.F12 -> Settings.debugPanelOpen = !Settings.debugPanelOpen
            Key.Spacebar ->
                PlayerRepository.playing.value?.let { playing ->
                    if (playing.value) PlayerRepository.pause() else PlayerRepository.play()
                }
            else -> return false
        }

        return true
    }
}
