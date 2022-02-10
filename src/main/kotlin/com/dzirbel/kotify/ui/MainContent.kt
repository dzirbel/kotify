package com.dzirbel.kotify.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.db.model.User
import com.dzirbel.kotify.db.model.UserRepository
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

data class ArtistPage(val artistId: String) : Page {
    fun titleFor(artist: Artist) = "Artist: ${artist.name}"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Artist(pageStack, this@ArtistPage)
    }
}

object AlbumsPage : Page {
    override fun toString() = "Saved Albums"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Albums(pageStack)
    }
}

data class AlbumPage(val albumId: String) : Page {
    fun titleFor(album: Album) = "Album: ${album.name}"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Album(pageStack, this@AlbumPage)
    }
}

data class PlaylistPage(val playlistId: String) : Page {
    fun titleFor(playlist: Playlist) = "Playlist: ${playlist.name}"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Playlist(pageStack, this@PlaylistPage)
    }
}

object LibraryStatePage : Page {
    override fun toString() = "Library State"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        LibraryState(pageStack)
    }
}

object TracksPage : Page {
    override fun toString() = "Saved Tracks"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Tracks(pageStack)
    }
}

private class AuthenticationMenuPresenter(scope: CoroutineScope) :
    Presenter<User?, AuthenticationMenuPresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load),
        initialState = null
    ) {

    sealed class Event {
        object Load : Event()
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                val user = UserRepository.getCurrentUser()
                user?.let {
                    KotifyDatabase.transaction { user.thumbnailImage.loadToCache() }
                }

                mutateState { user }
            }
        }
    }
}

@Composable
fun MainContent(pageStack: MutableState<PageStack>) {
    Column {
        val page = pageStack.value.current
        val headerVisibleState = remember(page) { MutableTransitionState(false) }

        Row(
            modifier = Modifier.fillMaxWidth().background(LocalColors.current.surface1),
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

            AnimatedVisibility(visibleState = headerVisibleState, enter = fadeIn(), exit = fadeOut()) {
                with(page) {
                    headerContent(pageStack)
                }
            }

            AuthenticationMenuHeader()
        }

        Box(Modifier.fillMaxSize().weight(1f)) {
            with(page) {
                content(pageStack = pageStack, toggleHeader = { headerVisibleState.targetState = it })
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

    val username = if (userError) "<ERROR>" else currentUser?.name ?: "<loading>"
    val expandedState = remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        ThemeSwitcher(modifier = Modifier.align(Alignment.CenterVertically))

        ProjectGithubIcon(modifier = Modifier.align(Alignment.CenterVertically))

        SimpleTextButton(
            enabled = currentUser != null || userError,
            onClick = { expandedState.value = !expandedState.value }
        ) {
            LoadedImage(
                url = currentUser?.thumbnailImage?.cached?.url,
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
