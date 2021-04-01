package com.dominiczirbel.ui

import androidx.compose.runtime.mutableStateOf
import com.dominiczirbel.network.Spotify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * A global object to expose the state of the Spotify player and allow changing the state from anywhere in the UI.
 */
object Player {
    val playable = mutableStateOf(false)

    private val _playEvents = MutableSharedFlow<Unit>()

    val playEvents: SharedFlow<Unit> = _playEvents.asSharedFlow()

    fun play(contextUri: String, scope: CoroutineScope = GlobalScope) {
        scope.launch {
            Spotify.Player.startPlayback(contextUri = contextUri)

            _playEvents.emit(Unit)
        }
    }
}
