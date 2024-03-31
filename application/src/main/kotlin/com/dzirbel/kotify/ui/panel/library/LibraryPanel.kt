package com.dzirbel.kotify.ui.panel.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.CacheStrategy
import com.dzirbel.kotify.repository.playlist.PlaylistViewModel
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.LocalPlayer
import com.dzirbel.kotify.ui.LocalPlaylistRepository
import com.dzirbel.kotify.ui.LocalSavedPlaylistRepository
import com.dzirbel.kotify.ui.components.LibraryInvalidateButton
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.TooltipArea
import com.dzirbel.kotify.ui.components.VerticalScroll
import com.dzirbel.kotify.ui.components.adapter.rememberListAdapterState
import com.dzirbel.kotify.ui.page.albums.AlbumsPage
import com.dzirbel.kotify.ui.page.artists.ArtistsPage
import com.dzirbel.kotify.ui.page.playlist.PlaylistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.properties.PlaylistLibraryOrderProperty
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.coroutines.combinedStateWhenAllNotNull
import com.dzirbel.kotify.util.coroutines.flatMapLatestIn
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun LibraryPanel(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, elevation = Dimens.panelElevationSmall) {
        VerticalScroll(Modifier.fillMaxHeight()) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.h5,
                modifier = Modifier.padding(start = Dimens.space3, end = Dimens.space3, top = Dimens.space3),
            )

            Divider(Modifier.padding(top = Dimens.space2))

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

            Spacer(Modifier.height(Dimens.space3))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(start = Dimens.space3, end = Dimens.space3, top = Dimens.space3),
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Playlists", style = MaterialTheme.typography.h5, maxLines = 1)

                    LibraryInvalidateButton(LocalSavedPlaylistRepository.current)
                }

                TooltipArea(
                    tooltip = "${Application.name} cannot access or manage Spotify playlist folders due to API " +
                        "limitations. In a future version, workarounds via local files created by Spotify's " +
                        "client may be added.",
                ) {
                    CachedIcon(
                        name = "folder",
                        modifier = Modifier.padding(Dimens.space2),
                        size = Dimens.iconSmall,
                    )
                }
            }

            Divider(Modifier.padding(top = Dimens.space2))

            val playlistRepository = LocalPlaylistRepository.current
            val savedPlaylistRepository = LocalSavedPlaylistRepository.current
            val playlists = rememberListAdapterState(defaultSort = PlaylistLibraryOrderProperty) { scope ->
                savedPlaylistRepository.library.flatMapLatestIn(scope) { cacheState ->
                    val ids = cacheState?.cachedValue?.ids
                    if (ids == null) {
                        MutableStateFlow(if (cacheState is CacheState.Error) emptyList() else null)
                    } else {
                        // TODO handle other cache states: shimmer when loading, show errors, etc
                        playlistRepository.statesOf(
                            ids = ids,
                            cacheStrategy = CacheStrategy.EntityTTL(), // do not require a full playlist model
                        ).combinedStateWhenAllNotNull { it?.cachedValue }
                    }
                }
            }

            if (playlists.value.hasElements) {
                for (playlist in playlists.value.sortedElements) {
                    PlaylistItem(playlist = playlist)
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
private fun PlaylistItem(playlist: PlaylistViewModel) {
    val selected = pageStack.value.current == PlaylistPage(playlistId = playlist.id)

    SimpleTextButton(
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        contentPadding = PaddingValues(horizontal = Dimens.space3, vertical = Dimens.space2),
        onClick = { pageStack.mutate { to(PlaylistPage(playlistId = playlist.id)) } },
    ) {
        Text(
            text = playlist.name,
            modifier = Modifier.weight(1f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )

        val playbackUriState = LocalPlayer.current.playbackContextUri.collectAsState()
        val playingPlaylist = remember(playlist.uri) {
            derivedStateOf { playlist.uri != null && playbackUriState.value == playlist.uri }
        }

        if (playingPlaylist.value) {
            CachedIcon(
                name = "volume-up",
                size = Dimens.fontDp,
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}

@Composable
private fun MaxWidthButton(text: String, selected: Boolean, onClick: () -> Unit) {
    SimpleTextButton(
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        contentPadding = PaddingValues(horizontal = Dimens.space3, vertical = Dimens.space2),
        onClick = onClick,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
