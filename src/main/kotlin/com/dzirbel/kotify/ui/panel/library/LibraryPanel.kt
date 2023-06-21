package com.dzirbel.kotify.ui.panel.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalScroll
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.framework.Presenter
import com.dzirbel.kotify.ui.page.albums.AlbumsPage
import com.dzirbel.kotify.ui.page.artists.ArtistsPage
import com.dzirbel.kotify.ui.page.library.LibraryStatePage
import com.dzirbel.kotify.ui.page.playlist.PlaylistPage
import com.dzirbel.kotify.ui.page.tracks.TracksPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.ui.util.mutate
import kotlinx.coroutines.Dispatchers

@Composable
fun LibraryPanel() {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { LibraryPanelPresenter(scope = scope) }

    LocalColors.current.WithSurface {
        VerticalScroll(Modifier.surfaceBackground()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.space3),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(start = Dimens.space3, end = Dimens.space3, top = Dimens.space3),
                )

                val moreExpanded = remember { mutableStateOf(false) }
                IconButton(
                    modifier = Modifier.size(Dimens.iconSmall),
                    onClick = { moreExpanded.value = true },
                ) {
                    CachedIcon(name = "more-vert", contentDescription = "More", size = Dimens.iconSmall)

                    DropdownMenu(
                        expanded = moreExpanded.value,
                        onDismissRequest = { moreExpanded.value = false },
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                moreExpanded.value = false
                                pageStack.mutate { to(LibraryStatePage) }
                            },
                        ) {
                            Text("Details")
                        }
                    }
                }
            }

            Box(Modifier.height(Dimens.divider).fillMaxWidth().background(LocalColors.current.dividerColor))

            VerticalSpacer(Dimens.space3)

            MaxWidthButton(
                text = "Artists",
                selected = pageStack.value.current == ArtistsPage,
                onClick = { pageStack.mutate { to(ArtistsPage) } },
            )

            MaxWidthButton(
                text = "Albums",
                selected = pageStack.value.current == AlbumsPage,
                onClick = { pageStack.mutate { to(AlbumsPage) } },
            )

            MaxWidthButton(
                text = "Songs",
                selected = pageStack.value.current == TracksPage,
                onClick = { pageStack.mutate { to(TracksPage) } },
            )

            VerticalSpacer(Dimens.space3)

            Text(
                modifier = Modifier.padding(start = Dimens.space3, end = Dimens.space3, top = Dimens.space3),
                style = MaterialTheme.typography.h5,
                text = "Playlists",
            )

            val stateOrError = presenter.state()
            val refreshing = stateOrError.safeState?.refreshing == true
            InvalidateButton(
                refreshing = refreshing,
                updated = stateOrError.safeState?.playlistsUpdated,
                contentPadding = PaddingValues(horizontal = Dimens.space3, vertical = Dimens.space2),
                onClick = {
                    presenter.emitAsync(LibraryPanelPresenter.Event.LoadPlaylists(invalidate = true))
                },
            )

            Box(Modifier.height(Dimens.divider).fillMaxWidth().background(LocalColors.current.dividerColor))

            VerticalSpacer(Dimens.space3)

            when (stateOrError) {
                is Presenter.StateOrError.Error ->
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconMedium).align(Alignment.CenterHorizontally),
                        tint = LocalColors.current.error,
                    )

                is Presenter.StateOrError.State -> {
                    val state = stateOrError.state
                    if (state != null) {
                        state.playlists.forEach { playlist -> PlaylistItem(playlist) }
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.iconMedium).align(Alignment.CenterHorizontally),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(playlist: Playlist) {
    val selected = pageStack.value.current == PlaylistPage(playlistId = playlist.id.value)

    SimpleTextButton(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = Dimens.space3, vertical = Dimens.space2),
        onClick = { pageStack.mutate { to(PlaylistPage(playlistId = playlist.id.value)) } },
    ) {
        Text(
            text = playlist.name,
            modifier = Modifier.weight(1f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )

        if (Player.playbackContext.value?.uri == playlist.uri) {
            CachedIcon(
                name = "volume-up",
                size = Dimens.fontBodyDp,
                contentDescription = "Volume",
                tint = LocalColors.current.primary,
            )
        }
    }
}

@Composable
private fun MaxWidthButton(
    text: String,
    selected: Boolean,
    contentPadding: PaddingValues = PaddingValues(all = Dimens.space3),
    onClick: () -> Unit,
) {
    SimpleTextButton(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        onClick = onClick,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
