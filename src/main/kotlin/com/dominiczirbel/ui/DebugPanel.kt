package com.dominiczirbel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.dominiczirbel.Logger
import com.dominiczirbel.cache.SpotifyCache
import com.dominiczirbel.cache.SpotifyImageCache
import com.dominiczirbel.network.DelayInterceptor
import com.dominiczirbel.network.Spotify
import com.dominiczirbel.ui.common.CheckboxWithLabel
import com.dominiczirbel.ui.common.SimpleTextButton
import com.dominiczirbel.ui.common.VerticalScroll
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.collectAsStateSwitchable
import com.dominiczirbel.ui.util.mutate
import com.dominiczirbel.util.formatByteSize
import com.dominiczirbel.util.formatDateTime
import kotlinx.coroutines.flow.map

private enum class DebugTab(val tabName: String, val log: Logger) {
    NETWORK("Network", Logger.Network),
    CACHE("Cache", Logger.Cache),
    IMAGE_CACHE("Image Cache", Logger.ImageCache)
}

private const val MAX_EVENTS = 500

private data class DebugState(
    val tab: DebugTab = DebugTab.values().first(),
    val filterApi: Boolean = false
)

@Composable
fun DebugPanel() {
    val debugState = remember { mutableStateOf(DebugState()) }

    Column {
        Column(Modifier.fillMaxHeight().weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                DebugTab.values().forEach { tab ->
                    TabButton(tab = tab, state = debugState)
                }
            }

            Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))

            Column(Modifier.fillMaxWidth().background(Colors.current.surface3).padding(Dimens.space3)) {
                when (debugState.value.tab) {
                    DebugTab.NETWORK -> NetworkOptions(debugState = debugState)
                    DebugTab.CACHE -> CacheOptions()
                    DebugTab.IMAGE_CACHE -> ImageCacheOptions()
                }
            }

            EventList(debugState.value)
        }

        Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))

        SimpleTextButton(
            onClick = { debugState.value.tab.log.clear() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear log")
        }
    }
}

@Composable
private fun RowScope.TabButton(tab: DebugTab, state: MutableState<DebugState>) {
    SimpleTextButton(
        onClick = { state.mutate { copy(tab = tab) } },
        modifier = Modifier.fillMaxWidth().weight(1f),
        backgroundColor = if (state.value.tab == tab) MaterialTheme.colors.primary else Color.Transparent
    ) {
        Text(tab.tabName)
    }
}

@Composable
private fun EventList(debugState: DebugState) {
    val filter = { event: Logger.Event ->
        when {
            debugState.tab == DebugTab.NETWORK && debugState.filterApi -> event.message.contains(Spotify.API_URL)
            else -> true
        }
    }

    val log = debugState.tab.log
    val events = log.eventsFlow
        .map { it.filter(filter).take(MAX_EVENTS) }
        .collectAsStateSwitchable(
            initial = { log.events.filter(filter).take(MAX_EVENTS) },
            key = debugState
        )

    VerticalScroll {
        events.value.forEachIndexed { index, event ->
            Column(Modifier.padding(Dimens.space2).fillMaxWidth()) {
                Row {
                    Icon(
                        imageVector = when (event.type) {
                            Logger.Event.Type.INFO -> Icons.Default.Info
                            Logger.Event.Type.SUCCESS -> Icons.Default.Check
                            Logger.Event.Type.WARNING -> Icons.Default.Warning
                            Logger.Event.Type.ERROR -> Icons.Default.Close
                        },
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall).align(Alignment.Top),
                        tint = when (event.type) {
                            Logger.Event.Type.INFO -> Colors.current.text
                            Logger.Event.Type.SUCCESS -> Color.Green
                            Logger.Event.Type.WARNING -> Color.Yellow
                            Logger.Event.Type.ERROR -> Colors.current.error
                        }
                    )

                    Spacer(Modifier.width(Dimens.space1))

                    Text(
                        text = event.message,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Spacer(Modifier.height(Dimens.space1))

                Text(
                    text = remember(event.time) { formatDateTime(event.time, includeMillis = true) },
                    fontSize = Dimens.fontCaption,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(Dimens.space2).align(Alignment.End)
                )
            }

            if (index != events.value.lastIndex) {
                Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))
            }
        }
    }
}

@Composable
private fun NetworkOptions(debugState: MutableState<DebugState>) {
    val delay = remember { mutableStateOf(DelayInterceptor.delayMs.toString()) }
    val appliedDelay = remember { mutableStateOf(true) }

    CheckboxWithLabel(
        modifier = Modifier.fillMaxWidth(),
        checked = debugState.value.filterApi,
        onCheckedChange = { debugState.mutate { copy(filterApi = it) } },
        label = { Text("API calls only") }
    )

    Spacer(Modifier.height(Dimens.space2))

    // TODO ideally we might reset to the actual delay value on un-focus
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = delay.value,
        singleLine = true,
        isError = !appliedDelay.value,
        onValueChange = { value ->
            delay.value = value

            value.toLongOrNull()
                ?.also { DelayInterceptor.delayMs = it }
                .also { appliedDelay.value = it != null }
        },
        label = {
            Text("Network delay (ms)", fontSize = Dimens.fontCaption)
        }
    )
}

@Composable
private fun CacheOptions() {
    val size = SpotifyCache.size
    val sizeOnDisk = SpotifyCache.sizeOnDisk
    val sizeOnDiskFormatted = remember(sizeOnDisk) { formatByteSize(sizeOnDisk) }

    Text("$size cached objects; $sizeOnDiskFormatted on disk")

    Spacer(Modifier.height(Dimens.space2))

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { SpotifyCache.clear() }
    ) {
        Text("Clear cache")
    }
}

@Composable
private fun ImageCacheOptions() {
    val inMemoryCount = SpotifyImageCache.state.inMemoryCount
    val diskCount = SpotifyImageCache.state.diskCount
    val totalDiskSize = SpotifyImageCache.state.totalDiskSize
    val totalSizeFormatted = remember(totalDiskSize) { formatByteSize(totalDiskSize.toLong()) }

    Text("$inMemoryCount images cached in memory; $diskCount cached on disk for a total of $totalSizeFormatted on disk")

    Spacer(Modifier.height(Dimens.space2))

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { SpotifyImageCache.clear() }
    ) {
        Text("Clear image cache")
    }
}
