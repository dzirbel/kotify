package com.dzirbel.kotify.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.cache.SpotifyImageCache
import com.dzirbel.kotify.network.DelayInterceptor
import com.dzirbel.kotify.ui.components.CheckboxWithLabel
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.VerticalScroll
import com.dzirbel.kotify.ui.components.VerticalSpacer
import com.dzirbel.kotify.ui.components.panel.FixedOrPercent
import com.dzirbel.kotify.ui.components.panel.PanelDirection
import com.dzirbel.kotify.ui.components.panel.PanelSize
import com.dzirbel.kotify.ui.components.panel.SidePanel
import com.dzirbel.kotify.ui.components.rightLeftClickable
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.Theme
import com.dzirbel.kotify.ui.util.collectAsStateSwitchable
import com.dzirbel.kotify.ui.util.mutate
import com.dzirbel.kotify.ui.util.setClipboard
import com.dzirbel.kotify.util.formatByteSize
import com.dzirbel.kotify.util.formatDateTime
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map

// TODO add tab for database operations
private enum class DebugTab(val tabName: String, val log: Logger<*>) {
    NETWORK("Network", Logger.Network),
    IMAGE_CACHE("Images", Logger.ImageCache),
    UI("UI", Logger.UI)
}

private const val MAX_EVENTS = 500

private data class NetworkSettings(
    val filterApi: Boolean = false,
    val filterIncoming: Boolean = false,
    val filterOutgoing: Boolean = false,
)

private data class ImageCacheSettings(
    val includeInMemory: Boolean = true,
    val includeOnDisk: Boolean = true,
    val includeMiss: Boolean = true,
)

private data class UISettings(
    val includeStates: Boolean = true,
    val includeEvents: Boolean = true,
    val includeErrors: Boolean = true,
    val presenterRegex: String = "",
)

// not part of the composition in order to retain values if the panel is hidden
private val tab = mutableStateOf(DebugTab.values().first())
private val networkSettings = mutableStateOf(NetworkSettings())
private val imageCacheSettings = mutableStateOf(ImageCacheSettings())
private val uiSettings = mutableStateOf(UISettings())
private val scrollStates = DebugTab.values().associateWith { ScrollState(0) }

private val debugPanelSize = PanelSize(
    initialSize = FixedOrPercent.Fixed(500.dp),
    minPanelSizeDp = 300.dp,
    minContentSizePercent = 0.5f,
)

/**
 * Wraps the debug panel in either a separate window or side panel if open, according to [Settings], and displays the
 * main [content].
 */
@Composable
fun DebugPanelOrWindow(content: @Composable () -> Unit) {
    if (Settings.debugPanelOpen) {
        if (Settings.debugPanelDetached) {
            content()

            Window(
                title = "${Application.name} debug tools",
                state = rememberWindowState(placement = WindowPlacement.Maximized),
                onCloseRequest = {
                    Settings.debugPanelOpen = false
                },
            ) {
                Theme.apply {
                    DebugPanelContent(Modifier.background(LocalColors.current.surface3))
                }
            }
        } else {
            SidePanel(
                direction = PanelDirection.RIGHT,
                panelSize = debugPanelSize,
                panelContent = { DebugPanelContent() },
                mainContent = content,
            )
        }
    } else {
        content()
    }
}

@Composable
private fun DebugPanelContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Column(Modifier.fillMaxHeight().weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = {
                        Settings.debugPanelDetached = !Settings.debugPanelDetached
                    }
                ) {
                    val detached = Settings.debugPanelDetached
                    CachedIcon(
                        name = if (detached) "view-sidebar" else "open-in-new",
                        modifier = Modifier.padding(horizontal = Dimens.space3),
                        size = Dimens.iconSmall,
                        contentDescription = if (detached) "Attach to sidebar" else "Open in new window",
                    )
                }

                DebugTab.values().forEach {
                    TabButton(tab = it, currentTab = tab)
                }
            }

            HorizontalDivider()

            when (tab.value) {
                DebugTab.NETWORK -> NetworkTab()
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
        backgroundColor = if (currentTab.value == tab) LocalColors.current.primary else Color.Transparent
    ) {
        Text(tab.tabName)
    }
}

@Composable
private fun NetworkTab() {
    Column(Modifier.fillMaxWidth().background(LocalColors.current.surface3).padding(Dimens.space3)) {
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
        (!networkSettings.value.filterApi || event.data.isSpotifyApi) &&
            (!networkSettings.value.filterIncoming || event.data.isResponse) &&
            (!networkSettings.value.filterOutgoing || event.data.isRequest)
    }
}

@Composable
private fun ImageCacheTab() {
    Column(Modifier.fillMaxWidth().background(LocalColors.current.surface3).padding(Dimens.space3)) {
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
        when (event.type) {
            Logger.Event.Type.SUCCESS -> imageCacheSettings.value.includeInMemory
            Logger.Event.Type.INFO -> imageCacheSettings.value.includeOnDisk
            Logger.Event.Type.WARNING -> imageCacheSettings.value.includeMiss
            else -> true
        }
    }
}

@Composable
private fun UITab() {
    Column(Modifier.fillMaxWidth().background(LocalColors.current.surface3).padding(Dimens.space3)) {
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

    val scrollState = scrollStates.getValue(DebugTab.UI)
    val presenterRegex = uiSettings.value.presenterRegex
        .takeIf { it.isNotEmpty() }
        ?.toRegex(RegexOption.IGNORE_CASE)
    EventList(log = Logger.UI, key = uiSettings.value, scrollState = scrollState) { event ->
        val includeEventType = when (event.data.type) {
            Logger.UI.EventType.STATE -> uiSettings.value.includeStates
            Logger.UI.EventType.EVENT -> uiSettings.value.includeEvents
            Logger.UI.EventType.ERROR -> uiSettings.value.includeErrors
        }

        if (includeEventType) {
            val presenterClass = event.data.presenterClass
            presenterClass != null && presenterRegex?.containsMatchIn(presenterClass) != false
        } else {
            false
        }
    }
}

@Composable
private fun <T> EventList(
    log: Logger<T>,
    key: Any,
    scrollState: ScrollState = rememberScrollState(0),
    filter: (Logger.Event<T>) -> Boolean = { true },
) {
    VerticalScroll(scrollState = scrollState) {
        val flow: SharedFlow<List<Logger.Event<T>>> = remember(key) { log.eventsFlow }
        val events: List<Logger.Event<T>> = flow
            .map { it.filter(filter).take(MAX_EVENTS) }
            .collectAsStateSwitchable(initial = { flow.replayCache.firstOrNull().orEmpty() }, key = key)
            .value

        events.asReversed().forEachIndexed { index, event ->
            EventItem(event)

            if (index != events.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun <T> EventItem(event: Logger.Event<T>) {
    val canExpand = !event.content.isNullOrBlank()

    // TODO doesn't retain state when adding new events to the list
    val expandedState = if (canExpand) remember(event) { mutableStateOf(false) } else null
    val expanded = expandedState?.value == true

    val rightClickMenuExpanded = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .let {
                if (canExpand) {
                    it.rightLeftClickable(
                        onLeftClick = { expandedState?.value = !expanded },
                        onRightClick = { rightClickMenuExpanded.value = true },
                    )
                } else {
                    it
                }
            }
            .padding(Dimens.space2)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.space1),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            DropdownMenu(
                expanded = rightClickMenuExpanded.value,
                onDismissRequest = { rightClickMenuExpanded.value = false },
            ) {
                DropdownMenuItem(
                    onClick = {
                        setClipboard(contents = event.content.orEmpty())
                        rightClickMenuExpanded.value = false
                    }
                ) {
                    Text("Copy contents to clipboard")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space1), modifier = Modifier.weight(1f)) {
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
                        Logger.Event.Type.INFO -> LocalColors.current.text
                        Logger.Event.Type.SUCCESS -> Color.Green
                        Logger.Event.Type.WARNING -> Color.Yellow
                        Logger.Event.Type.ERROR -> LocalColors.current.error
                    },
                )

                Text(
                    text = event.title,
                    fontFamily = FontFamily.Monospace,
                )
            }

            if (canExpand) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
            }
        }

        if (expanded) {
            Text(
                text = event.content.orEmpty(),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = Dimens.space3),
            )
        }

        Text(
            text = remember(event.time) { formatDateTime(event.time, includeMillis = true) },
            fontSize = Dimens.fontCaption,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(Dimens.space2).align(Alignment.End),
        )
    }
}
