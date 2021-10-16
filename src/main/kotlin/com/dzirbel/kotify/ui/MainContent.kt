package com.dzirbel.kotify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.network.model.Album
import com.dzirbel.kotify.network.model.Artist
import com.dzirbel.kotify.network.model.Playlist
import com.dzirbel.kotify.network.model.PrivateUser
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object ArtistsPage : Page {
    override fun toString() = "Saved Artists"
}

data class ArtistPage(val artistId: String) : Page {
    fun titleFor(artist: Artist) = "Artist: ${artist.name}"
}

object AlbumsPage : Page {
    override fun toString() = "Saved Albums"
}

data class AlbumPage(val albumId: String) : Page {
    fun titleFor(album: Album) = "Album: ${album.name}"
}

data class PlaylistPage(val playlistId: String) : Page {
    fun titleFor(playlist: Playlist) = "Playlist: ${playlist.name}"
}

object LibraryStatePage : Page {
    override fun toString() = "Library State"
}

object TracksPage : Page {
    override fun toString() = "Saved Tracks"
}

private class AuthenticationMenuPresenter(scope: CoroutineScope) :
    Presenter<PrivateUser?, AuthenticationMenuPresenter.Event>(
        scope = scope,
        eventMergeStrategy = EventMergeStrategy.LATEST,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    sealed class Event {
        object Load : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                val user = SpotifyCache.UsersProfile.getCurrentUser()

                mutateState { user }
            }
        }
    }
}

@Composable
fun MainContent(pageStack: MutableState<PageStack>) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(Colors.current.surface1),
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
                        modifier = Modifier.size(Dimens.iconMedium)
                    )
                }

                val historyExpanded = remember { mutableStateOf(false) }
                IconButton(
                    enabled = pageStack.value.pages.size > 1,
                    onClick = { historyExpanded.value = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "History",
                        modifier = Modifier.size(Dimens.iconMedium)
                    )

                    DropdownMenu(
                        expanded = historyExpanded.value,
                        onDismissRequest = { historyExpanded.value = false }
                    ) {
                        pageStack.value.pages.forEachIndexed { index, page ->
                            DropdownMenuItem(
                                onClick = {
                                    historyExpanded.value = false
                                    pageStack.mutate { toIndex(index) }
                                },
                                enabled = index != pageStack.value.currentIndex
                            ) {
                                Text(pageStack.value.pageTitles[index] ?: page.toString())
                            }
                        }
                    }
                }

                IconButton(
                    enabled = pageStack.value.hasNext,
                    onClick = { pageStack.mutate { toNext() } }
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        modifier = Modifier.size(Dimens.iconMedium)
                    )
                }
            }

            AuthenticationMenuHeader()
        }

        Box(Modifier.fillMaxSize().weight(1f)) {
            when (val current = pageStack.value.current) {
                ArtistsPage -> Artists(pageStack)
                AlbumsPage -> Albums(pageStack)
                TracksPage -> Tracks(pageStack)
                is AlbumPage -> Album(pageStack, current)
                is ArtistPage -> Artist(pageStack, current)
                is PlaylistPage -> Playlist(pageStack, current)
                LibraryStatePage -> LibraryState(pageStack)
                else -> error("unknown page type: ${pageStack.value.current}")
            }
        }
    }
}

@Composable
private fun AuthenticationMenuHeader() {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { AuthenticationMenuPresenter(scope = scope) }

    val currentUser = presenter.state().safeState
    val userError = presenter.state() is Presenter.StateOrError.Error

    val username = if (userError) "<ERROR>" else currentUser?.displayName ?: "<loading>"
    val expandedState = remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        ThemeSwitcher(modifier = Modifier.align(Alignment.CenterVertically))

        ProjectGithubIcon(modifier = Modifier.align(Alignment.CenterVertically))

        SimpleTextButton(
            enabled = currentUser != null || userError,
            onClick = { expandedState.value = !expandedState.value }
        ) {
            LoadedImage(
                url = currentUser?.images?.firstOrNull()?.url,
                modifier = Modifier.size(Dimens.iconMedium)
            )

            HorizontalSpacer(Dimens.space2)

            Text(
                text = username,
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            HorizontalSpacer(Dimens.space2)

            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Expand",
                modifier = Modifier.requiredSize(Dimens.iconMedium).align(Alignment.CenterVertically)
            )

            DropdownMenu(
                expanded = expandedState.value,
                onDismissRequest = { expandedState.value = false }
            ) {
                AuthenticationMenu(user = currentUser)
            }
        }
    }
}
