package com.dominiczirbel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.theme.disabled
import com.dominiczirbel.ui.util.mutate

object ArtistsPage : Page {
    override fun toString() = "artists"
}

object AlbumsPage : Page {
    override fun toString() = "albums"
}

object TracksPage : Page {
    override fun toString() = "tracks"
}

@Composable
fun MainContent(pageStack: MutableState<PageStack>) {
    Column {
        Row(Modifier.fillMaxWidth().background(Colors.current.panelBackground).padding(Dimens.space2)) {
            IconButton(
                enabled = pageStack.value.hasPrevious,
                onClick = { pageStack.mutate { toPrevious() } }
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    modifier = Modifier.requiredSize(Dimens.iconMedium),
                    tint = Colors.current.text.let {
                        if (pageStack.value.hasPrevious) it else it.disabled()
                    }
                )
            }

            IconButton(
                enabled = pageStack.value.hasNext,
                onClick = { pageStack.mutate { toNext() } }
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    modifier = Modifier.requiredSize(Dimens.iconMedium),
                    tint = Colors.current.text.let {
                        if (pageStack.value.hasNext) it else it.disabled()
                    }
                )
            }

            Text(
                text = "Stack: [${pageStack.value.pages.joinToString(separator = ", ")}] | " +
                    "current: ${pageStack.value.currentIndex}",
                color = Colors.current.text,
                fontSize = Dimens.fontBody,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }

        Box(Modifier.fillMaxSize()) {
            when (pageStack.value.current) {
                ArtistsPage -> Artists()
                AlbumsPage -> Albums()
                TracksPage -> Tracks()
                else -> error("unknown page type: ${pageStack.value.current}")
            }
        }
    }
}
