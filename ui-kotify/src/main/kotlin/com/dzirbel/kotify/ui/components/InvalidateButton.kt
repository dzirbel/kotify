package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.dzirbel.kotify.repository2.CacheState
import com.dzirbel.kotify.repository2.Repository
import com.dzirbel.kotify.repository2.SavedRepository
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.instrumentation.instrument

/**
 * Either an indefinite progress indicator when [refreshing] is true, otherwise a refresh icon.
 */
@Composable
fun RefreshIcon(refreshing: Boolean, size: Dp = Dimens.iconMedium) {
    if (refreshing) {
        CircularProgressIndicator(Modifier.size(size))
    } else {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Refresh",
            modifier = Modifier.size(size),
        )
    }
}

/**
 * A [SimpleTextButton] which handles the common case of invalidating a cached resource.
 */
@Composable
fun InvalidateButton(
    refreshing: Boolean,
    updated: Long?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Dimens.space3),
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    updatedFormat: (String) -> String = { "Synced $it" },
    updatedFallback: String = "Never synced",
    onClick: () -> Unit,
) {
    SimpleTextButton(
        modifier = modifier.instrument(),
        enabled = !refreshing,
        contentPadding = contentPadding,
        onClick = onClick,
    ) {
        Text(
            fontSize = fontSize,
            text = updated?.let {
                liveRelativeDateText(timestamp = updated, format = updatedFormat)
            } ?: updatedFallback,
        )

        HorizontalSpacer(Dimens.space2)

        RefreshIcon(refreshing = refreshing, size = Dimens.iconSizeFor(fontSize))
    }
}

/**
 * A wrapper around [InvalidateButton] which reflects the [CacheState] of the entity with the given [id] in the given
 * [repository]
 *
 * TODO stability with Repository param
 */
@Composable
fun InvalidateButton(
    repository: Repository<*>,
    id: String,
    entityName: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Dimens.space3),
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
) {
    val cacheState = repository.stateOf(id = id).collectAsState().value
    InvalidateButton(
        refreshing = cacheState is CacheState.Refreshing,
        updated = cacheState?.cacheTime?.toEpochMilli(),
        updatedFormat = { "$entityName synced $it" },
        updatedFallback = "$entityName never synced",
        onClick = { repository.refreshFromRemote(id = id) },
        modifier = modifier,
        contentPadding = contentPadding,
        fontSize = fontSize,
    )
}

/**
 * A wrapper around [InvalidateButton] which reflects the state of the library for the given [savedRepository].
 */
@Composable
fun LibraryInvalidateButton(
    savedRepository: SavedRepository,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Dimens.space3),
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
) {
    val libraryState = savedRepository.library.collectAsState()
    val cacheTimeState = remember {
        derivedStateOf { libraryState.value?.cacheTime }
    }

    InvalidateButton(
        refreshing = savedRepository.libraryRefreshing.collectAsState().value,
        updated = cacheTimeState.value?.toEpochMilli(),
        onClick = savedRepository::refreshLibrary,
        modifier = modifier,
        contentPadding = contentPadding,
        fontSize = fontSize,
    )
}
