package com.dzirbel.kotify.ui.page.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.ui.components.AlbumCell
import com.dzirbel.kotify.ui.components.Interpunct
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.adapter.Divider
import com.dzirbel.kotify.ui.components.adapter.DividerSelector
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortSelector
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.compare
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.framework.PageLoadingSpinner
import com.dzirbel.kotify.ui.theme.Dimens

val SortAlbumsByName = object : SortableProperty<Album>(
    sortTitle = "Album Name",
    defaultOrder = SortOrder.ASCENDING,
    terminal = true,
) {
    override fun compare(sortOrder: SortOrder, first: IndexedValue<Album>, second: IndexedValue<Album>): Int {
        return sortOrder.compare(first.value.name, second.value.name)
    }
}

object AlbumNameDivider : Divider<Album>(dividerTitle = "Name") {
    override fun compareDivisions(sortOrder: SortOrder, first: Any, second: Any): Int {
        return sortOrder.compare(first as String, second as String)
    }

    override fun divisionFor(element: Album): String {
        val firstChar = element.name[0]
        return if (firstChar.isLetter()) firstChar.uppercaseChar().toString() else "#"
    }
}

@Composable
fun ArtistPageHeader(presenter: ArtistPresenter, state: ArtistPresenter.ViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.space5, vertical = Dimens.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(state.artist?.name.orEmpty(), style = MaterialTheme.typography.h4)

            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    if (state.artistAlbums.hasElements) {
                        Text(
                            text = "${state.artistAlbums.size} albums",
                            modifier = Modifier.padding(end = Dimens.space2),
                        )

                        Interpunct()
                    }

                    if (state.artist != null) {
                        InvalidateButton(
                            contentPadding = PaddingValues(all = Dimens.space2),
                            refreshing = state.refreshingArtist,
                            updated = state.artist.updatedTime.toEpochMilli(),
                            updatedFormat = { "Artist synced $it" },
                            updatedFallback = "Artist never synced",
                            onClick = {
                                presenter.emitAsync(
                                    ArtistPresenter.Event.Load(
                                        refreshArtist = true,
                                        invalidateArtist = true,
                                        refreshArtistAlbums = false,
                                        invalidateArtistAlbums = false
                                    )
                                )
                            }
                        )

                        Interpunct()

                        InvalidateButton(
                            contentPadding = PaddingValues(all = Dimens.space2),
                            refreshing = state.refreshingArtistAlbums,
                            // TODO doesn't update when only the albums are synced (but artist model is not)
                            updated = state.artist.albumsFetched?.toEpochMilli(),
                            updatedFormat = { "Albums synced $it" },
                            updatedFallback = "Albums never synced",
                            onClick = {
                                presenter.emitAsync(
                                    ArtistPresenter.Event.Load(
                                        refreshArtist = false,
                                        invalidateArtist = false,
                                        refreshArtistAlbums = true,
                                        invalidateArtistAlbums = true
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
            DividerSelector(
                dividers = listOf(AlbumNameDivider),
                currentDivider = state.artistAlbums.divider,
                currentDividerSortOrder = state.artistAlbums.dividerSortOrder,
                onSelectDivider = { divider, dividerSortOrder ->
                    presenter.emitAsync(
                        ArtistPresenter.Event.SetDivider(divider = divider, dividerSortOrder = dividerSortOrder)
                    )
                },
            )

            SortSelector(
                sortProperties = listOf(SortAlbumsByName),
                sorts = state.artistAlbums.sorts.orEmpty(),
                onSetSort = { presenter.emitAsync(ArtistPresenter.Event.SetSorts(sorts = it)) },
            )
        }
    }
}

@Composable
fun ArtistPageContent(presenter: ArtistPresenter, state: ArtistPresenter.ViewModel) {
    if (state.artistAlbums.hasElements) {
        Grid(
            elements = state.artistAlbums,
            edgePadding = PaddingValues(
                start = Dimens.space5 - Dimens.space3,
                end = Dimens.space5 - Dimens.space3,
                bottom = Dimens.space3,
            ),
        ) { _, album ->
            AlbumCell(
                album = album,
                isSaved = state.savedAlbumsState?.value?.contains(album.id.value),
                showRating = true,
                ratings = state.albumRatings[album.id.value]?.map { it.value },
                onToggleSave = { save ->
                    presenter.emitAsync(ArtistPresenter.Event.ToggleSave(albumId = album.id.value, save = save))
                }
            )
        }
    } else {
        PageLoadingSpinner()
    }
}
