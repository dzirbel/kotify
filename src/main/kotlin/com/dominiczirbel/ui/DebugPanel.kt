package com.dominiczirbel.ui

import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.Icon
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

private data class NetworkSettings(
    val filterApi: Boolean = false,
    val filterIncoming: Boolean = false,
    val filterOutgoing: Boolean = false
)

private data class CacheSettings(
    val includeLoad: Boolean = true,
    val includeSave: Boolean = true,
    val includeDump: Boolean = true,
    val includeClear: Boolean = true,
    val includeHit: Boolean = true,
    val includeMiss: Boolean = true,
    val includePut: Boolean = true,
    val includeInvalidate: Boolean = true
)

private data class ImageCacheSettings(
    val includeInMemory: Boolean = true,
    val includeOnDisk: Boolean = true,
    val includeMiss: Boolean = true
)

@Composable
fun DebugPanel() {
    val tab = remember { mutableStateOf(DebugTab.values().first()) }
    val networkSettings = remember { mutableStateOf(NetworkSettings()) }
    val cacheSettings = remember { mutableStateOf(CacheSettings()) }
    val imageCacheSettings = remember { mutableStateOf(ImageCacheSettings()) }
    val scrollStates = DebugTab.values().associate { it to rememberScrollState(0) }

    Column {
        Column(Modifier.fillMaxHeight().weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                DebugTab.values().forEach {
                    TabButton(tab = it, currentTab = tab)
                }
            }

            Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))

            when (tab.value) {
                DebugTab.NETWORK -> NetworkTab(networkSettings, scrollStates.getValue(tab.value))
                DebugTab.CACHE -> CacheTab(cacheSettings, scrollStates.getValue(tab.value))
                DebugTab.IMAGE_CACHE -> ImageCacheTab(imageCacheSettings, scrollStates.getValue(tab.value))
            }
        }

        Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))

        SimpleTextButton(
            onClick = { tab.value.log.clear() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear log")
        }
    }
}

@Composable
private fun RowScope.TabButton(tab: DebugTab, currentTab: MutableState<DebugTab>) {
    SimpleTextButton(
        onClick = { currentTab.value = tab },
        modifier = Modifier.fillMaxWidth().weight(1f),
        backgroundColor = if (currentTab.value == tab) Colors.current.primary else Color.Transparent
    ) {
        Text(tab.tabName)
    }
}

@Composable
private fun NetworkTab(networkSettings: MutableState<NetworkSettings>, scrollState: ScrollState) {
    Column(Modifier.fillMaxWidth().background(Colors.current.surface3).padding(Dimens.space3)) {
        val delay = remember { mutableStateOf(DelayInterceptor.delayMs.toString()) }
        val appliedDelay = remember { mutableStateOf(true) }

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

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = networkSettings.value.filterApi,
            onCheckedChange = { networkSettings.mutate { copy(filterApi = it) } },
            label = { Text("Spotify API calls only") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = networkSettings.value.filterIncoming,
            onCheckedChange = { networkSettings.mutate { copy(filterIncoming = it) } },
            label = { Text("Incoming responses only") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = networkSettings.value.filterOutgoing,
            onCheckedChange = { networkSettings.mutate { copy(filterOutgoing = it) } },
            label = { Text("Outgoing requests only") }
        )
    }

    EventList(log = Logger.Network, key = networkSettings.value, scrollState = scrollState) { event ->
        var allow = true

        if (networkSettings.value.filterApi) {
            allow = allow && event.message.contains(Spotify.API_URL)
        }

        if (networkSettings.value.filterIncoming) {
            allow = allow && event.message.startsWith("<< ")
        }

        if (networkSettings.value.filterOutgoing) {
            allow = allow && event.message.startsWith(">> ")
        }

        allow
    }
}

@Composable
private fun CacheTab(cacheSettings: MutableState<CacheSettings>, scrollState: ScrollState) {
    Column(Modifier.fillMaxWidth().background(Colors.current.surface3).padding(Dimens.space3)) {
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

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeLoad,
            onCheckedChange = { cacheSettings.mutate { copy(includeLoad = it) } },
            label = { Text("Include LOAD events") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeSave,
            onCheckedChange = { cacheSettings.mutate { copy(includeSave = it) } },
            label = { Text("Include SAVE events") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeDump,
            onCheckedChange = { cacheSettings.mutate { copy(includeDump = it) } },
            label = { Text("Include DUMP events") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeClear,
            onCheckedChange = { cacheSettings.mutate { copy(includeClear = it) } },
            label = { Text("Include CLEAR events") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeHit,
            onCheckedChange = { cacheSettings.mutate { copy(includeHit = it) } },
            label = { Text("Include HIT events") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeMiss,
            onCheckedChange = { cacheSettings.mutate { copy(includeMiss = it) } },
            label = { Text("Include MISS events") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includePut,
            onCheckedChange = { cacheSettings.mutate { copy(includePut = it) } },
            label = { Text("Include PUT events") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeInvalidate,
            onCheckedChange = { cacheSettings.mutate { copy(includeInvalidate = it) } },
            label = { Text("Include INVALIDATE events") }
        )
    }

    EventList(log = Logger.Cache, key = cacheSettings.value, scrollState = scrollState) { event ->
        when {
            event.message.startsWith("LOAD") -> cacheSettings.value.includeLoad
            event.message.startsWith("SAVE") -> cacheSettings.value.includeSave
            event.message.startsWith("DUMP") -> cacheSettings.value.includeDump
            event.message.startsWith("CLEAR") -> cacheSettings.value.includeClear
            event.message.startsWith("HIT") -> cacheSettings.value.includeHit
            event.message.startsWith("MISS") -> cacheSettings.value.includeMiss
            event.message.startsWith("PUT") -> cacheSettings.value.includePut
            event.message.startsWith("INVALIDATE") -> cacheSettings.value.includeInvalidate
            else -> true
        }
    }
}

@Composable
private fun ImageCacheTab(imageCacheSettings: MutableState<ImageCacheSettings>, scrollState: ScrollState) {
    Column(Modifier.fillMaxWidth().background(Colors.current.surface3).padding(Dimens.space3)) {
        val inMemoryCount = SpotifyImageCache.state.inMemoryCount
        val diskCount = SpotifyImageCache.state.diskCount
        val totalDiskSize = SpotifyImageCache.state.totalDiskSize
        val totalSizeFormatted = remember(totalDiskSize) { formatByteSize(totalDiskSize.toLong()) }

        Text(
            "$inMemoryCount images cached in memory; " +
                "$diskCount cached on disk for a total of $totalSizeFormatted on disk"
        )

        Spacer(Modifier.height(Dimens.space2))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { SpotifyImageCache.clear() }
        ) {
            Text("Clear image cache")
        }

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = imageCacheSettings.value.includeInMemory,
            onCheckedChange = { imageCacheSettings.mutate { copy(includeInMemory = it) } },
            label = { Text("Include IN-MEMORY events") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = imageCacheSettings.value.includeOnDisk,
            onCheckedChange = { imageCacheSettings.mutate { copy(includeOnDisk = it) } },
            label = { Text("Include ON-DISK events") }
        )

        Spacer(Modifier.height(Dimens.space2))

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = imageCacheSettings.value.includeMiss,
            onCheckedChange = { imageCacheSettings.mutate { copy(includeMiss = it) } },
            label = { Text("Include MISS events") }
        )
    }

    EventList(log = Logger.ImageCache, key = imageCacheSettings.value, scrollState = scrollState) { event ->
        when {
            event.message.startsWith("IN-MEMORY") -> imageCacheSettings.value.includeInMemory
            event.message.startsWith("ON-DISK") -> imageCacheSettings.value.includeOnDisk
            event.message.startsWith("MISS") -> imageCacheSettings.value.includeMiss
            else -> true
        }
    }
}

@Composable
private fun EventList(
    log: Logger,
    key: Any,
    scrollState: ScrollState = rememberScrollState(0),
    filter: (Logger.Event) -> Boolean = { true }
) {
    val events = log.eventsFlow
        .map { it.filter(filter).take(MAX_EVENTS) }
        .collectAsStateSwitchable(initial = { log.events.filter(filter).take(MAX_EVENTS) }, key = key)
        .value

    VerticalScroll(scrollState = scrollState) {
        events.forEachIndexed { index, event ->
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

            if (index != events.lastIndex) {
                Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))
            }
        }
    }
}
