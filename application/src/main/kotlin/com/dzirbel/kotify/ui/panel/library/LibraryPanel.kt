package com.dzirbel.kotify.ui.panel.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dzirbel.kotify.db.model.Playlist
import com.dzirbel.kotify.repository2.player.PlayerRepository
import com.dzirbel.kotify.repository2.playlist.PlaylistRepository
import com.dzirbel.kotify.repository2.playlist.SavedPlaylistRepository
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.LibraryInvalidateButton
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalScroll
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.page.albums.AlbumsPage
import com.dzirbel.kotify.ui.page.artists.ArtistsPage
import com.dzirbel.kotify.ui.page.library.LibraryStatePage
import com.dzirbel.kotify.ui.page.playlist.PlaylistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.zipEach

@Composable
fun LibraryPanel() {
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

            HorizontalDivider(Modifier.padding(bottom = Dimens.space3))

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

            VerticalSpacer(Dimens.space3)

            Text(
                modifier = Modifier.padding(start = Dimens.space3, end = Dimens.space3, top = Dimens.space3),
                style = MaterialTheme.typography.h5,
                text = "Playlists",
            )

            LibraryInvalidateButton(SavedPlaylistRepository)

            HorizontalDivider(Modifier.padding(bottom = Dimens.space3))

            val savedPlaylistIds = SavedPlaylistRepository.library.collectAsState().value?.ids
            if (savedPlaylistIds != null) {
                val playlistStates = remember(savedPlaylistIds) {
                    PlaylistRepository.statesOf(ids = savedPlaylistIds)
                }

                savedPlaylistIds.zipEach(playlistStates) { playlistId, playlistState ->
                    key(playlistId) {
                        val playlist = playlistState.collectAsState().value?.cachedValue

                        // TODO ideally handle other cache states: shimmer when loading, show errors, etc
                        if (playlist != null) {
                            PlaylistItem(playlist = playlist)
                        }
                    }
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimens.iconMedium).align(Alignment.CenterHorizontally),
                )
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

        val playbackUriState = PlayerRepository.playbackContextUri.collectAsState()
        val playingPlaylist = remember(playlist.uri) {
            derivedStateOf { playbackUriState.value == playlist.uri }
        }

        if (playingPlaylist.value) {
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
