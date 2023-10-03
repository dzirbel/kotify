package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.repository.DataSource
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.SpotifyImageCache
import com.dzirbel.kotify.ui.components.ToggleButtonGroup
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.util.capitalize
import com.dzirbel.kotify.util.coroutines.lockedState
import com.dzirbel.kotify.util.formatByteSize
import com.dzirbel.kotify.util.takingIf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.runningFold

@Composable
fun ImageCacheTab() {
    val selectedDataSources = remember { mutableStateOf(persistentSetOf<DataSource>()) }

    // do not filter if all or none are selected (no-op filter)
    val filterDataSource = selectedDataSources.value.size in 1 until DataSource.entries.size

    LogList(
        log = SpotifyImageCache.log,
        display = ImageCacheLogEventDisplay,
        filter = takingIf(filterDataSource) {
            @Suppress("Wrapping") // ktlint false positive; fixed by https://github.com/pinterest/ktlint/pull/2127
            { it.event.data in selectedDataSources.value }
        },
        filterKey = selectedDataSources.value,
        onResetFilter = { selectedDataSources.value = persistentSetOf() },
        canResetFilter = selectedDataSources.value.isNotEmpty(),
    ) { eventCleared ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(Dimens.space3),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2),
        ) {
            val metrics = SpotifyImageCache.metricsFlow.collectAsState().value
            if (metrics != null) {
                val totalSizeFormatted = remember(metrics.totalDiskSize) { formatByteSize(metrics.totalDiskSize) }

                Text(
                    "${metrics.inMemoryCount} images cached in memory; " +
                        "${metrics.diskCount} cached on disk for a total of $totalSizeFormatted on disk",
                )
            } else {
                Text("Image cache metrics loading...")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { SpotifyImageCache.clear() },
            ) {
                Text("Clear image cache")
            }

            ToggleButtonGroup(
                elements = DataSource.entries.toImmutableList(),
                selectedElements = selectedDataSources.value,
                onSelectElements = { selectedDataSources.value = it },
                content = { dataSource ->
                    CachedIcon(name = dataSource.iconName, size = Dimens.iconSmall)

                    Spacer(Modifier.width(Dimens.space2))

                    val scope = rememberCoroutineScope()
                    val name = dataSource.name.lowercase().capitalize()
                    val count: Int? = remember(eventCleared) {
                        SpotifyImageCache.log.writeLock.lockedState(
                            scope = scope,
                            initializeWithLock = {
                                SpotifyImageCache.log.events.count { !eventCleared(it) && it.data == dataSource }
                            },
                        ) { initial ->
                            SpotifyImageCache.log.eventsFlow.runningFold(initial) { count, event ->
                                if (!eventCleared(event) && event.data == dataSource) count + 1 else count
                            }
                        }
                    }
                        .collectAsState()
                        .value

                    Text("$name [$count]", maxLines = 1)
                },
            )
        }
    }
}

private object ImageCacheLogEventDisplay : LogEventDisplay<DataSource> {
    @Composable
    override fun Icon(event: Log.Event<DataSource>, modifier: Modifier) {
        CachedIcon(name = event.data.iconName, modifier = modifier, tint = event.type.iconColor)
    }
}
