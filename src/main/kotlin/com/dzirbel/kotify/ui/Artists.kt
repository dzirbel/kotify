package com.dzirbel.kotify.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.db.KotifyDatabase
import com.dzirbel.kotify.db.model.Album
import com.dzirbel.kotify.db.model.Artist
import com.dzirbel.kotify.db.model.ArtistRepository
import com.dzirbel.kotify.db.model.SavedAlbumRepository
import com.dzirbel.kotify.db.model.SavedArtistRepository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.ui.components.Flow
import com.dzirbel.kotify.ui.components.Grid
import com.dzirbel.kotify.ui.components.InvalidateButton
import com.dzirbel.kotify.ui.components.LoadedImage
import com.dzirbel.kotify.ui.components.Page
import com.dzirbel.kotify.ui.components.PageStack
import com.dzirbel.kotify.ui.components.Pill
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.rightLeftClickable
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.plusSorted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import java.time.Instant

object ArtistsPage : Page {
    override fun toString() = "Saved Artists"

    @Composable
    override fun BoxScope.content(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
        Artists(pageStack, toggleHeader)
    }

    @Composable
    override fun RowScope.headerContent(pageStack: MutableState<PageStack>) {
        Text("Artists")
    }
}

private class ArtistsPresenter(scope: CoroutineScope) :
    RemoteStatePresenter<ArtistsPresenter.ViewModel, ArtistsPresenter.Event>(
        scope = scope,
        startingEvents = listOf(Event.Load(invalidate = false)),
    ) {

    data class ArtistDetails(
        val savedTime: Instant?,
        val genres: List<String>,
        val albums: List<Album>?,
    )

    data class ViewModel(
        val refreshing: Boolean,
        val artists: List<Artist>,
        val artistDetails: Map<String, ArtistDetails>,
        val savedArtistIds: Set<String>,
        val savedAlbumsState: State<Set<String>?>? = null,
        val artistsUpdated: Long?,
    )

    sealed class Event {
        data class Load(val invalidate: Boolean) : Event()
        data class LoadArtistDetails(val artistId: String) : Event()
        data class ReactToArtistsSaved(val artistIds: List<String>, val saved: Boolean) : Event()
        data class ToggleSave(val artistId: String, val save: Boolean) : Event()
        data class ToggleAlbumSaved(val albumId: String, val save: Boolean) : Event()
    }

    override fun eventFlows(): Iterable<Flow<Event>> {
        return listOf(
            SavedArtistRepository.eventsFlow()
                .filterIsInstance<SavedRepository.Event.SetSaved>()
                .map { Event.ReactToArtistsSaved(artistIds = it.ids, saved = it.saved) },

            SavedArtistRepository.eventsFlow()
                .filterIsInstance<SavedRepository.Event.QueryLibraryRemote>()
                .map { Event.Load(invalidate = false) },
        )
    }

    override suspend fun reactTo(event: Event) {
        when (event) {
            is Event.Load -> {
                mutateLoadedState { it.copy(refreshing = true) }

                if (event.invalidate) {
                    SavedArtistRepository.invalidateLibrary()
                }

                val savedArtistIds = SavedArtistRepository.getLibrary()
                val artists = fetchArtists(artistIds = savedArtistIds.toList())
                    .sortedBy { it.name }
                val artistsUpdated = SavedArtistRepository.libraryUpdated()

                initializeLoadedState {
                    ViewModel(
                        refreshing = false,
                        artists = artists,
                        artistDetails = it?.artistDetails.orEmpty(),
                        savedArtistIds = savedArtistIds,
                        artistsUpdated = artistsUpdated?.toEpochMilli(),
                    )
                }
            }

            is Event.LoadArtistDetails -> {
                // don't load details again if already in the state
                if (queryState { it.viewModel?.artistDetails }?.containsKey(event.artistId) == true) return

                val artist = queryState { it.viewModel?.artists }?.find { it.id.value == event.artistId }
                    ?: ArtistRepository.getCached(id = event.artistId)
                requireNotNull(artist) { "could not resolve artist for ${event.artistId}" }

                val savedTime = SavedArtistRepository.savedTimeCached(id = event.artistId)
                val genres = KotifyDatabase
                    .transaction { artist.genres.live }
                    .map { it.name }
                    .sorted()

                val details = ArtistDetails(
                    savedTime = savedTime,
                    genres = genres,
                    albums = null
                )

                mutateLoadedState {
                    it.copy(artistDetails = it.artistDetails.plus(event.artistId to details))
                }

                val albums = Artist.getAllAlbums(artistId = event.artistId)
                KotifyDatabase.transaction {
                    albums.forEach { it.largestImage.loadToCache() }
                }

                val savedAlbumsState = if (queryState { it.viewModel?.savedAlbumsState } == null) {
                    SavedAlbumRepository.libraryState()
                } else {
                    null
                }

                mutateLoadedState {
                    it.copy(
                        artistDetails = it.artistDetails.plus(event.artistId to details.copy(albums = albums)),
                        savedAlbumsState = savedAlbumsState ?: it.savedAlbumsState,
                    )
                }
            }

            is Event.ReactToArtistsSaved -> {
                if (event.saved) {
                    // if an artist has been saved but is now missing from the grid of artists, load and add it
                    val stateArtists = queryState { it.viewModel?.artists }.orEmpty()

                    val missingArtistIds: List<String> = event.artistIds
                        .minus(stateArtists.mapTo(mutableSetOf()) { it.id.value })

                    if (missingArtistIds.isNotEmpty()) {
                        val missingArtists = fetchArtists(artistIds = missingArtistIds)
                        val allArtists = stateArtists.plusSorted(missingArtists) { it.name }

                        mutateLoadedState {
                            it.copy(artists = allArtists, savedArtistIds = it.savedArtistIds.plus(event.artistIds))
                        }
                    } else {
                        mutateLoadedState {
                            it.copy(savedArtistIds = it.savedArtistIds.plus(event.artistIds))
                        }
                    }
                } else {
                    // if an artist has been unsaved, retain the grid of artists but toggle its save state
                    mutateLoadedState {
                        it.copy(savedArtistIds = it.savedArtistIds.minus(event.artistIds.toSet()))
                    }
                }
            }

            is Event.ToggleSave -> SavedArtistRepository.setSaved(id = event.artistId, saved = event.save)

            is Event.ToggleAlbumSaved -> SavedAlbumRepository.setSaved(id = event.albumId, saved = event.save)
        }
    }

    /**
     * Loads the full [Artist] objects from the [ArtistRepository] and does common initialization - caching their images
     * from the database and warming the image cache.
     */
    private suspend fun fetchArtists(artistIds: List<String>): List<Artist> {
        val artists = ArtistRepository.getFull(ids = artistIds).filterNotNull()

        val imageUrls = KotifyDatabase.transaction {
            artists.mapNotNull { artist ->
                artist.largestImage.live?.url
            }
        }
        SpotifyImageCache.loadFromFileCache(urls = imageUrls, scope = scope)

        return artists
    }
}

@Composable
private fun BoxScope.Artists(pageStack: MutableState<PageStack>, toggleHeader: (Boolean) -> Unit) {
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
                Text(
                    modifier = Modifier.padding(Dimens.space4),
                    text = "Artists",
                    fontSize = Dimens.fontTitle,
                )

                InvalidateButton(
                    refreshing = state.refreshing,
                    updated = state.artistsUpdated,
                    onClick = { presenter.emitAsync(ArtistsPresenter.Event.Load(invalidate = true)) }
                )
            }
        },
        onHeaderVisibilityChanged = { toggleHeader(!it) },
    ) { state ->
        val selectedArtist = remember { mutableStateOf<Artist?>(null) }
        Grid(
            elements = state.artists,
            selectedElement = selectedArtist.value,
            detailInsertContent = { artist ->
                ArtistDetailInsert(artist = artist, presenter = presenter, state = state, pageStack = pageStack)
            },
        ) { artist ->
            ArtistCell(
                artist = artist,
                savedArtists = state.savedArtistIds,
                presenter = presenter,
                pageStack = pageStack,
                onRightClick = {
                    presenter.emitAsync(ArtistsPresenter.Event.LoadArtistDetails(artistId = artist.id.value))
                    selectedArtist.value = artist.takeIf { selectedArtist.value != it }
                }
            )
        }
    }
}

@Composable
private fun ArtistCell(
    artist: Artist,
    savedArtists: Set<String>,
    presenter: ArtistsPresenter,
    pageStack: MutableState<PageStack>,
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
    pageStack: MutableState<PageStack>,
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
                    pageStack = pageStack,
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
