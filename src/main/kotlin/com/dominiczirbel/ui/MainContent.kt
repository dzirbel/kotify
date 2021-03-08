package com.dominiczirbel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.theme.disabled
import com.dominiczirbel.ui.util.RemoteState
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
        Row(
            modifier = Modifier.fillMaxWidth().background(Colors.current.panelBackground),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.padding(Dimens.space2)) {
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

            DebugMenuHeader()
        }

        Box(Modifier.fillMaxSize().weight(1f)) {
            when (pageStack.value.current) {
                ArtistsPage -> Artists()
                AlbumsPage -> Albums()
                TracksPage -> Tracks()
                else -> error("unknown page type: ${pageStack.value.current}")
            }
        }
    }
}

@Composable
private fun DebugMenuHeader() {
    val currentUserState = RemoteState.of { SpotifyCache.UsersProfile.getCurrentUser() }
    val currentUser = (currentUserState as? RemoteState.Success)?.data
    val username = currentUser?.displayName ?: "<loading>"
    val expandedState = remember { mutableStateOf(false) }

    TextButton(
        enabled = currentUser != null,
        onClick = { expandedState.value = !expandedState.value }
    ) {
        Text(
            text = "Authenticated as $username",
            color = Colors.current.text,
            fontSize = Dimens.fontBody,
            modifier = Modifier.align(Alignment.CenterVertically)
        )

        Spacer(Modifier.width(Dimens.space2))

        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = "Expand",
            modifier = Modifier.requiredSize(Dimens.iconMedium).align(Alignment.CenterVertically),
            tint = Colors.current.text
        )

        currentUser?.let {
            DropdownMenu(
                expanded = expandedState.value,
                onDismissRequest = { expandedState.value = false }
            ) {
                DebugMenu(user = currentUser)
            }
        }
    }
}
