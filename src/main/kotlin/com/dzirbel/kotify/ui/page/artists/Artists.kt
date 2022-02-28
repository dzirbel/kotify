package com.dzirbel.kotify.ui.page.artists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.ui.components.Flow
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.Pill
import com.dzirbel.kotify.ui.components.PlayButton
import com.dzirbel.kotify.ui.components.SmallAlbumCell
import com.dzirbel.kotify.ui.components.ToggleSaveButton
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortSelector
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.grid.DivisionSelector
import com.dzirbel.kotify.ui.components.grid.Grid
import com.dzirbel.kotify.ui.components.grid.GridElements
import com.dzirbel.kotify.ui.components.grid.GridWithDivisions
import com.dzirbel.kotify.ui.components.grid.SimpleGridDivider
import com.dzirbel.kotify.ui.components.rightLeftClickable
import com.dzirbel.kotify.ui.framework.StandardPage
import com.dzirbel.kotify.ui.page.artist.ArtistPage
import com.dzirbel.kotify.ui.pageStack
import com.dzirbel.kotify.ui.player.Player
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.compareToNullable
import kotlinx.coroutines.Dispatchers

val SortArtistByName = object : SortableProperty<Artist>(
    sortTitle = "Artist Name",
    defaultOrder = SortOrder.ASCENDING,
) {
    override fun compare(first: IndexedValue<Artist>, second: IndexedValue<Artist>): Int {
        return first.value.name.compareTo(second.value.name)
    }
}

val SortArtistByPopularity = object : SortableProperty<Artist>(
    sortTitle = "Artist Popularity",
    defaultOrder = SortOrder.DESCENDING,
) {
    override fun compare(first: IndexedValue<Artist>, second: IndexedValue<Artist>): Int {
        return first.value.popularity.compareToNullable(second.value.popularity)
    }
}

data class ArtistNameDivider(
    override val sortOrder: SortOrder = SortOrder.ASCENDING,
) : SimpleGridDivider<Artist>("Name") {
    override fun divisionFor(element: Artist): String {
        val firstChar = element.name[0]
        return if (firstChar.isLetter()) firstChar.uppercaseChar().toString() else "#"
    }

    override fun withSortOrder(sortOrder: SortOrder) = copy(sortOrder = sortOrder)
}

// TODO temp divider for testing
data class ArtistNameDivider2(
    override val sortOrder: SortOrder = SortOrder.ASCENDING,
) : SimpleGridDivider<Artist>("Last Char") {
    override fun divisionFor(element: Artist): String {
        val lastChar = element.name.last()
        return if (lastChar.isLetter()) lastChar.uppercaseChar().toString() else "#"
    }

    override fun withSortOrder(sortOrder: SortOrder) = copy(sortOrder = sortOrder)
}

@Composable
fun BoxScope.Artists(toggleHeader: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val presenter = remember { ArtistsPresenter(scope = scope) }

    StandardPage(
        scrollState = pageStack.value.currentScrollState,
        presenter = presenter,
        header = { state ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.space4),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.space4),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space2),
                ) {
                    Text(
                        text = "Artists",
                        fontSize = Dimens.fontHuge,
                        fontWeight = FontWeight.Bold,
                    )

                    SortSelector(
                        sortProperties = listOf(
                            SortArtistByName,
                            SortArtistByPopularity,
                        ),
                        sorts = state.sorts,
                        onSetSort = {
                            presenter.emitAsync(ArtistsPresenter.Event.SetSorts(sorts = it))
                        },
                    )

                    DivisionSelector(
                        dividers = listOf(ArtistNameDivider(), ArtistNameDivider2()),
                        currentDivider = state.divider,
                        onSelectDivider = {
                            presenter.emitAsync(ArtistsPresenter.Event.SetDivider(divider = it))
                        },
                    )
                }

                InvalidateButton(
                    refreshing = state.refreshing,
                    updated = state.artistsUpdated,
                    onClick = { presenter.emitAsync(ArtistsPresenter.Event.Load(invalidate = true)) }
                )
            }
        },
        onHeaderVisibilityChanged = { toggleHeader(!it) },
    ) { state ->
        val selectedArtist = remember(state.sorts, state.divider) { mutableStateOf<Artist?>(null) }

        when (state.artists) {
            is GridElements.PlainList ->
                Grid(
                    elements = state.artists.elements,
                    selectedElement = selectedArtist.value,
                    detailInsertContent = { artist ->
                        ArtistDetailInsert(artist = artist, presenter = presenter, state = state)
                    },
                ) { artist ->
                    ArtistCell(
                        artist = artist,
                        savedArtists = state.savedArtistIds,
                        presenter = presenter,
                        onRightClick = {
                            presenter.emitAsync(ArtistsPresenter.Event.LoadArtistDetails(artistId = artist.id.value))
                            selectedArtist.value = artist.takeIf { selectedArtist.value != it }
                        }
                    )
                }

            is GridElements.DividedList<Artist> ->
                GridWithDivisions(
                    elements = state.artists,
                    selectedElement = selectedArtist.value,
                    detailInsertContent = { artist ->
                        ArtistDetailInsert(artist = artist, presenter = presenter, state = state)
                    },
                ) { artist ->
                    ArtistCell(
                        artist = artist,
                        savedArtists = state.savedArtistIds,
                        presenter = presenter,
                        onRightClick = {
                            presenter.emitAsync(ArtistsPresenter.Event.LoadArtistDetails(artistId = artist.id.value))
                            selectedArtist.value = artist.takeIf { selectedArtist.value != it }
                        }
                    )
                }
        }
    }
}

@Composable
private fun ArtistCell(
    artist: Artist,
    savedArtists: Set<String>,
    presenter: ArtistsPresenter,
    onRightClick: () -> Unit,
) {
    Column(
        Modifier
            .clip(RoundedCornerShape(Dimens.cornerSize))
            .rightLeftClickable(
                onLeftClick = {
                    pageStack.mutate { to(ArtistPage(artistId = artist.id.value)) }
                },
                onRightClick = onRightClick,
            )
            .padding(Dimens.space3)
    ) {
        LoadedImage(
            url = artist.largestImage.cached?.url,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        VerticalSpacer(Dimens.space3)

        Row(
            modifier = Modifier.widthIn(max = Dimens.contentImage),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            Text(text = artist.name, modifier = Modifier.weight(1f))

            val isSaved = savedArtists.contains(artist.id.value)
            ToggleSaveButton(isSaved = isSaved) {
                presenter.emitAsync(ArtistsPresenter.Event.ToggleSave(artistId = artist.id.value, save = !isSaved))
            }

            PlayButton(context = Player.PlayContext.artist(artist), size = Dimens.iconSmall)
        }
    }
}

private const val DETAILS_COLUMN_WEIGHT = 0.3f
private const val DETAILS_ALBUMS_WEIGHT = 0.7f

@Composable
private fun ArtistDetailInsert(
    artist: Artist,
    presenter: ArtistsPresenter,
    state: ArtistsPresenter.ViewModel,
) {
    Row(modifier = Modifier.padding(Dimens.space4), horizontalArrangement = Arrangement.spacedBy(Dimens.space3)) {
        val artistDetails = state.artistDetails[artist.id.value]

        LoadedImage(url = artist.largestImage.cached?.url)

        Column(
            modifier = Modifier.weight(weight = DETAILS_COLUMN_WEIGHT),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            Text(artist.name, fontSize = Dimens.fontTitle)

            artistDetails?.let {
                artistDetails.savedTime?.let { savedTime ->
                    Text("Saved $savedTime") // TODO improve datetime formatting
                }

                Flow {
                    artistDetails.genres.forEach { genre ->
                        Pill(text = genre)
                    }
                }
            }
        }

        artistDetails?.albums?.let { albums ->
            Grid(
                modifier = Modifier.weight(DETAILS_ALBUMS_WEIGHT),
                elements = albums,
            ) { album ->
                SmallAlbumCell(
                    album = album,
                    isSaved = state.savedAlbumsState?.value?.contains(album.id.value),
                    onToggleSave = { save ->
                        presenter.emitAsync(
                            ArtistsPresenter.Event.ToggleAlbumSaved(albumId = album.id.value, save = save)
                        )
                    }
                )
            }
        }
    }
}
