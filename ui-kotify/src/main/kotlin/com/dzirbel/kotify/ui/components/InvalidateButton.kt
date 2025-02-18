package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.repository.CacheState
import com.dzirbel.kotify.repository.Repository
import com.dzirbel.kotify.repository.SavedRepository
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.RelativeTimeInfo
import com.dzirbel.kotify.ui.util.instrumentation.instrument
import com.dzirbel.kotify.ui.util.intrinsicSize

@Composable
fun LibraryInvalidateButton(
    savedRepository: SavedRepository,
    modifier: Modifier = Modifier,
    icon: String? = null,
    contentPadding: PaddingValues = PaddingValues(Dimens.space1),
) {
    InvalidateButton(
        cacheState = savedRepository.library.collectAsState().value,
        onClick = savedRepository::refreshLibrary,
        entityName = "${savedRepository.entityName} library",
        modifier = modifier,
        icon = icon,
        contentPadding = contentPadding,
    )
}

@Composable
fun InvalidateButton(
    repository: Repository<*>,
    id: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    contentPadding: PaddingValues = PaddingValues(Dimens.space1),
) {
    InvalidateButton(
        cacheState = repository.stateOf(id = id).collectAsState().value,
        onClick = { repository.refreshFromRemote(id = id) },
        entityName = repository.entityName,
        modifier = modifier,
        icon = icon,
        contentPadding = contentPadding,
    )
}

@Composable
fun <T> InvalidateButton(
    cacheState: CacheState<T>?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    entityName: String? = null,
    contentPadding: PaddingValues = PaddingValues(Dimens.space1),
) {
    InvalidateButton(
        refreshing = cacheState is CacheState.Refreshing,
        updated = cacheState?.cacheTime?.toEpochMilli(),
        onClick = onClick,
        modifier = modifier,
        error = cacheState is CacheState.Error,
        icon = icon,
        entityName = entityName,
        contentPadding = contentPadding,
    )
}

@Composable
fun InvalidateButton(
    refreshing: Boolean,
    updated: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    icon: String? = null,
    contentPadding: PaddingValues = PaddingValues(Dimens.space1),
    entityName: String? = null,
    // TODO include TTL(s) in tooltip
    tooltip: (RelativeTimeInfo?) -> String = { relativeTime ->
        relativeTime?.let { "Last fetched ${entityName?.plus(' ').orEmpty()}from Spotify ${it.formatLong()}" }
            ?: "Never fetched ${entityName?.plus(' ').orEmpty()}from Spotify"
    },
) {
    val relativeTime = updated?.let { liveRelativeTime(timestamp = updated) }

    TooltipArea(tooltip = tooltip(relativeTime), delayMillis = TOOLTIP_DELAY_LONG, modifier = modifier) {
        SimpleTextButton(
            modifier = Modifier.instrument(),
            enabled = !refreshing,
            onClick = onClick,
            contentPadding = contentPadding,
            enforceMinWidth = false,
        ) {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space1),
            ) {
                if (icon != null) {
                    CachedIcon(name = icon, size = null, modifier = Modifier.fillMaxHeight().aspectRatio(1f))
                }

                if (refreshing) {
                    // hack: CircularProgressIndicator has a hardcoded size() which will prevent its intrinsic size from
                    // ever being overridden, so we wrap in a Box with a forced intrinsic size of zero and also force
                    // the width to equal the height (which will be specified by the row to match the minimum intrinsic
                    // height, i.e. the text height)
                    Box(
                        modifier = Modifier
                            .intrinsicSize(minWidth = 0.dp, minHeight = 0.dp)
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxHeight))
                                layout(placeable.width, placeable.height) {
                                    placeable.place(0, 0)
                                }
                            },
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp, strokeCap = StrokeCap.Round)
                    }
                } else {
                    CachedIcon(
                        name = when {
                            error -> "error"
                            relativeTime == null -> "cloud-off"
                            else -> "cloud-download"
                        },
                        size = null,
                        modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                        tint = if (error) MaterialTheme.colors.error else LocalContentColor.current,
                    )
                }

                Text(text = relativeTime?.formatShort() ?: "never", maxLines = 1)
            }
        }
    }
}
