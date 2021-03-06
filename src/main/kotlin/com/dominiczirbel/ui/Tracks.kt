package com.dominiczirbel.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.RemoteState

@Composable
fun BoxScope.Tracks() {
    val state = RemoteState.of {
        SpotifyCache.Tracks.getSavedTracks().map { SpotifyCache.Tracks.getTrack(it) }
    }

    ScrollingPage(state) { tracks ->
        Column {
            Text("Tracks", color = Colors.current.text, fontSize = Dimens.fontTitle)

            Spacer(Modifier.height(Dimens.space3))

            tracks.forEach { track ->
                Text(
                    text = track.name,
                    color = Colors.current.text,
                    fontSize = Dimens.fontBody,
                    modifier = Modifier.padding(vertical = Dimens.space2)
                )
            }
        }
    }
}
