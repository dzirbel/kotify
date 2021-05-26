package com.dzirbel.kotify.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.cache.SpotifyCache
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.network.DelayInterceptor
import com.dzirbel.kotify.network.Spotify
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalScroll
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.util.collectAsStateSwitchable
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.util.formatByteSize
import com.dzirbel.kotify.util.formatDateTime
import kotlinx.coroutines.flow.map

private enum class DebugTab(val tabName: String, val log: Logger) {
    NETWORK("Network", Logger.Network),
    CACHE("Cache", Logger.Cache),
    IMAGE_CACHE("Images", Logger.ImageCache),
    UI("UI", Logger.UI)
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

private data class UISettings(
    val includeStates: Boolean = true,
    val includeEvents: Boolean = true,
    val includeErrors: Boolean = true,
    val presenterRegex: String = ""
)

// not part of the composition in order to retain values if the panel is hidden
private val tab = mutableStateOf(DebugTab.values().first())
private val networkSettings = mutableStateOf(NetworkSettings())
private val cacheSettings = mutableStateOf(CacheSettings())
private val imageCacheSettings = mutableStateOf(ImageCacheSettings())
private val uiSettings = mutableStateOf(UISettings())
private val scrollStates = DebugTab.values().associate { it to ScrollState(0) }

@Composable
fun DebugPanel() {
    Column {
        Column(Modifier.fillMaxHeight().weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                DebugTab.values().forEach {
                    TabButton(tab = it, currentTab = tab)
                }
            }

            HorizontalDivider()

            when (tab.value) {
                DebugTab.NETWORK -> NetworkTab()
                DebugTab.CACHE -> CacheTab()
                DebugTab.IMAGE_CACHE -> ImageCacheTab()
                DebugTab.UI -> UITab()
            }
        }

        HorizontalDivider()

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
private fun NetworkTab() {
    Column(Modifier.fillMaxWidth().background(Colors.current.surface3).padding(Dimens.space3)) {
        val delay = remember { mutableStateOf(DelayInterceptor.delayMs.toString()) }
        val appliedDelay = remember { mutableStateOf(true) }

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.hasFocus) {
                        delay.value = DelayInterceptor.delayMs.toString()
                        appliedDelay.value = true
                    }
                },
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

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = networkSettings.value.filterApi,
            onCheckedChange = { networkSettings.mutate { copy(filterApi = it) } },
            label = { Text("Spotify API calls only") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = networkSettings.value.filterIncoming,
            onCheckedChange = { networkSettings.mutate { copy(filterIncoming = it) } },
            label = { Text("Incoming responses only") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = networkSettings.value.filterOutgoing,
            onCheckedChange = { networkSettings.mutate { copy(filterOutgoing = it) } },
            label = { Text("Outgoing requests only") }
        )
    }

    val scrollState = scrollStates.getValue(DebugTab.NETWORK)
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
private fun CacheTab() {
    Column(Modifier.fillMaxWidth().background(Colors.current.surface3).padding(Dimens.space3)) {
        val size = SpotifyCache.size
        val sizeOnDisk = SpotifyCache.sizeOnDisk
        val sizeOnDiskFormatted = remember(sizeOnDisk) { formatByteSize(sizeOnDisk) }

        Text("$size cached objects; $sizeOnDiskFormatted on disk")

        VerticalSpacer(Dimens.space2)

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { SpotifyCache.clear() }
        ) {
            Text("Clear cache")
        }

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeLoad,
            onCheckedChange = { cacheSettings.mutate { copy(includeLoad = it) } },
            label = { Text("Include LOAD events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeSave,
            onCheckedChange = { cacheSettings.mutate { copy(includeSave = it) } },
            label = { Text("Include SAVE events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeDump,
            onCheckedChange = { cacheSettings.mutate { copy(includeDump = it) } },
            label = { Text("Include DUMP events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeClear,
            onCheckedChange = { cacheSettings.mutate { copy(includeClear = it) } },
            label = { Text("Include CLEAR events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeHit,
            onCheckedChange = { cacheSettings.mutate { copy(includeHit = it) } },
            label = { Text("Include HIT events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeMiss,
            onCheckedChange = { cacheSettings.mutate { copy(includeMiss = it) } },
            label = { Text("Include MISS events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includePut,
            onCheckedChange = { cacheSettings.mutate { copy(includePut = it) } },
            label = { Text("Include PUT events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = cacheSettings.value.includeInvalidate,
            onCheckedChange = { cacheSettings.mutate { copy(includeInvalidate = it) } },
            label = { Text("Include INVALIDATE events") }
        )
    }

    val scrollState = scrollStates.getValue(DebugTab.CACHE)
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
private fun ImageCacheTab() {
    Column(Modifier.fillMaxWidth().background(Colors.current.surface3).padding(Dimens.space3)) {
        val inMemoryCount = SpotifyImageCache.state.inMemoryCount
        val diskCount = SpotifyImageCache.state.diskCount
        val totalDiskSize = SpotifyImageCache.state.totalDiskSize
        val totalSizeFormatted = remember(totalDiskSize) { formatByteSize(totalDiskSize.toLong()) }

        Text(
            "$inMemoryCount images cached in memory; " +
                "$diskCount cached on disk for a total of $totalSizeFormatted on disk"
        )

        VerticalSpacer(Dimens.space2)

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { SpotifyImageCache.clear() }
        ) {
            Text("Clear image cache")
        }

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = imageCacheSettings.value.includeInMemory,
            onCheckedChange = { imageCacheSettings.mutate { copy(includeInMemory = it) } },
            label = { Text("Include IN-MEMORY events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = imageCacheSettings.value.includeOnDisk,
            onCheckedChange = { imageCacheSettings.mutate { copy(includeOnDisk = it) } },
            label = { Text("Include ON-DISK events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = imageCacheSettings.value.includeMiss,
            onCheckedChange = { imageCacheSettings.mutate { copy(includeMiss = it) } },
            label = { Text("Include MISS events") }
        )
    }

    val scrollState = scrollStates.getValue(DebugTab.IMAGE_CACHE)
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
private fun UITab() {
    Column(Modifier.fillMaxWidth().background(Colors.current.surface3).padding(Dimens.space3)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiSettings.value.presenterRegex,
            singleLine = true,
            onValueChange = {
                uiSettings.mutate { copy(presenterRegex = it) }
            },
            label = {
                Text("Presenter class regex (case insensitive)", fontSize = Dimens.fontCaption)
            }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = uiSettings.value.includeStates,
            onCheckedChange = { uiSettings.mutate { copy(includeStates = it) } },
            label = { Text("Include state changes") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = uiSettings.value.includeEvents,
            onCheckedChange = { uiSettings.mutate { copy(includeEvents = it) } },
            label = { Text("Include events") }
        )

        VerticalSpacer(Dimens.space2)

        CheckboxWithLabel(
            modifier = Modifier.fillMaxWidth(),
            checked = uiSettings.value.includeErrors,
            onCheckedChange = { uiSettings.mutate { copy(includeErrors = it) } },
            label = { Text("Include errors") }
        )
    }

    val eventMessageRegex = remember { """\[(.*)] (.*) -> .*""".toRegex() }
    val scrollState = scrollStates.getValue(DebugTab.UI)
    EventList(log = Logger.UI, key = uiSettings.value, scrollState = scrollState) { event ->
        val match = eventMessageRegex.matchEntire(event.message)!!

        val includeEventType = when (val eventType = match.groupValues[2]) {
            "State" -> uiSettings.value.includeStates
            "Event" -> uiSettings.value.includeEvents
            "Error" -> uiSettings.value.includeErrors
            else -> error("unexpected event type: $eventType")
        }

        if (includeEventType) {
            val presenterRegex = uiSettings.value.presenterRegex
                .takeIf { it.isNotEmpty() }
                ?.toRegex(RegexOption.IGNORE_CASE)

            val presenterClass = match.groupValues[1]
            presenterRegex?.containsMatchIn(presenterClass) != false
        } else {
            false
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

                    HorizontalSpacer(Dimens.space1)

                    Text(
                        text = event.message,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                VerticalSpacer(Dimens.space1)

                Text(
                    text = remember(event.time) { formatDateTime(event.time, includeMillis = true) },
                    fontSize = Dimens.fontCaption,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(Dimens.space2).align(Alignment.End)
                )
            }

            if (index != events.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
