package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dzirbel.contextmenu.ContextMenuIcon
import com.dzirbel.contextmenu.MaterialContextMenuItem
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.HorizontalSpacer
import com.dzirbel.kotify.ui.components.LazyVerticalScroll
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.SortSelector
import com.dzirbel.kotify.ui.components.ToggleButtonGroup
import com.dzirbel.kotify.ui.components.adapter.Sort
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.SortableProperty
import com.dzirbel.kotify.ui.components.adapter.asComparator
import com.dzirbel.kotify.ui.components.adapter.compareBy
import com.dzirbel.kotify.ui.components.adapter.compareByNullable
import com.dzirbel.kotify.ui.components.liveRelativeDateText
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyTypography
import com.dzirbel.kotify.ui.util.ProvidingDisabledContentAlpha
import com.dzirbel.kotify.ui.util.setClipboard
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.capitalize
import com.dzirbel.kotify.util.collections.mapLazy
import com.dzirbel.kotify.util.coroutines.MergedMutex
import com.dzirbel.kotify.util.coroutines.lockedListState
import com.dzirbel.kotify.util.coroutines.lockedState
import com.dzirbel.kotify.util.coroutines.mergeFlows
import com.dzirbel.kotify.util.time.formatShortDuration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.sync.Mutex
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class LogAndEvent<T>(val log: Log<T>, val event: Log.Event<T>)

@Composable
fun <T> LogList(
    log: Log<T>,
    display: LogEventDisplay<T> = object : LogEventDisplay<T> {},
    sortProperties: ImmutableList<SortableProperty<Log.Event<T>>> = persistentListOf(),
    filter: ((LogAndEvent<T>) -> Boolean)? = null,
    filterKey: Any? = filter,
    onResetFilter: () -> Unit = {},
    canResetFilter: Boolean = false,
    headerContent: @Composable ((eventCleared: (Log.Event<T>) -> Boolean) -> Unit)? = null,
) {
    LogList(
        logs = listOf(log),
        logMutex = log.writeLock,
        display = display,
        sortProperties = sortProperties,
        filter = filter,
        filterKey = filterKey,
        onResetFilter = onResetFilter,
        canResetFilter = canResetFilter,
        annotateTitlesByLog = false,
        headerContent = headerContent,
    )
}

// TODO add separators for page changes
@Composable
fun <T> LogList(
    logs: Iterable<Log<T>>,
    logMutex: Mutex = remember(logs) { MergedMutex(logs.map { it.writeLock }) },
    display: LogEventDisplay<T> = object : LogEventDisplay<T> {},
    sortProperties: ImmutableList<SortableProperty<Log.Event<T>>> = persistentListOf(),
    filter: ((LogAndEvent<T>) -> Boolean)? = null,
    filterKey: Any? = filter,
    onResetFilter: () -> Unit = {},
    canResetFilter: Boolean = false,
    annotateTitlesByLog: Boolean = logs.count() > 1,
    headerContent: @Composable ((eventCleared: (Log.Event<T>) -> Boolean) -> Unit)? = null,
) {
    val selectedEventTypes = remember { mutableStateOf(persistentSetOf<Log.Event.Type>()) }

    val sortableProperties = remember(sortProperties) {
        persistentListOf<SortableProperty<Log.Event<T>>>(EventTimeProperty(), EventDurationProperty())
            .addAll(sortProperties)
    }
    val sorts = remember { mutableStateOf(persistentListOf(Sort(sortableProperties.first()))) }

    val clearTimeState = remember { mutableStateOf<Long?>(null) }
    val clearTime = clearTimeState.value

    val eventCleared: (Log.Event<T>) -> Boolean = remember(clearTime) {
        if (clearTime == null) {
            { false }
        } else {
            { event -> event.time <= clearTime }
        }
    }

    val scope = rememberCoroutineScope()

    val numEvents: Int? = remember(logs, clearTime) {
        logMutex.lockedState(
            scope = scope,
            initializeWithLock = {
                if (clearTime == null) {
                    logs.sumOf { it.events.size }
                } else {
                    logs.sumOf { log -> log.events.count { event -> event.time > clearTime } }
                }
            },
        ) { initial ->
            logs.mergeFlows { it.eventsFlow }
                .runningFold(initial = initial) { count, event ->
                    if (eventCleared(event)) count else count + 1
                }
        }
    }
        .collectAsState()
        .value

    val countsByType: IntArray? = remember(logs, clearTime) {
        val countsByType = IntArray(Log.Event.Type.entries.size)
        logMutex.lockedState(
            scope = scope,
            initializeWithLock = {
                for (log in logs) {
                    for (event in log.events) {
                        if (!eventCleared(event)) countsByType[event.type.ordinal]++
                    }
                }
                countsByType
            },
        ) { initial ->
            logs.mergeFlows { it.eventsFlow }
                .runningFold(initial) { counts, event ->
                    if (!eventCleared(event)) counts[event.type.ordinal]++
                    counts
                }
        }
    }
        .collectAsState()
        .value

    val visibleEvents: List<LogAndEvent<T>> = remember(
        logs,
        selectedEventTypes.value,
        filterKey,
        sorts.value,
        clearTime,
    ) {
        // do not filter if all or none are selected (no-op filter)
        val filterEventType = selectedEventTypes.value.size in 1 until Log.Event.Type.entries.size

        logMutex.lockedListState(
            scope = scope,
            initializeWithLock = {
                logs.flatMap { log -> log.events.mapLazy { event -> LogAndEvent(log, event) } }
            },
            sort = Comparator.comparing({ logAndEvent -> logAndEvent.event }, sorts.value.asComparator()),
            filter = { logAndEvent ->
                val event = logAndEvent.event
                (!filterEventType || selectedEventTypes.value.contains(event.type)) &&
                    !eventCleared(event) &&
                    filter?.invoke(logAndEvent) != false
            },
        ) {
            logs.mergeFlows { log -> log.eventsFlow.map { event -> LogAndEvent(log, event) } }
        }
    }
        .collectAsState()
        .value

    val annotatedDisplay = if (annotateTitlesByLog) {
        object : LogEventDisplay<T> by display {
            override fun title(log: Log<T>, event: Log.Event<T>) = "[${log.name}] ${display.title(log, event)}"
        }
    } else {
        display
    }

    Surface {
        Column {
            Surface(elevation = Dimens.componentElevation) {
                Column {
                    if (headerContent != null) {
                        headerContent(eventCleared)

                        HorizontalDivider()
                    }

                    Column(Modifier.padding(Dimens.space2), verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ToggleButtonGroup(
                                elements = Log.Event.Type.entries.toImmutableList(),
                                selectedElements = selectedEventTypes.value,
                                onSelectElements = { selectedEventTypes.value = it },
                                content = { type ->
                                    val count = countsByType?.get(type.ordinal)
                                    val name = type.name.lowercase().capitalize()
                                    Text("$name [$count]", maxLines = 1)
                                },
                            )

                            // include both internal and external filters
                            val canResetFilterFinal = canResetFilter || selectedEventTypes.value.isNotEmpty()
                            SimpleTextButton(
                                enabled = canResetFilterFinal,
                                onClick = {
                                    selectedEventTypes.value = persistentSetOf()
                                    onResetFilter()
                                },
                            ) {
                                if (canResetFilterFinal) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(Dimens.iconTiny),
                                    )

                                    HorizontalSpacer(Dimens.space1)
                                }

                                Text("${visibleEvents.size}/$numEvents visible", maxLines = 1)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SortSelector(
                                sortableProperties = sortableProperties,
                                sorts = sorts.value,
                                onSetSort = { sorts.value = it },
                            )

                            Row {
                                SimpleTextButton(onClick = { clearTimeState.value = CurrentTime.millis }) {
                                    CachedIcon("delete", size = Dimens.iconSmall)
                                    HorizontalSpacer(Dimens.space1)
                                    Text("Clear", maxLines = 1)
                                }

                                val canRestore = clearTime != null
                                ProvidingDisabledContentAlpha(disabled = !canRestore) {
                                    SimpleTextButton(onClick = { clearTimeState.value = null }, enabled = canRestore) {
                                        CachedIcon("restore-from-trash", size = Dimens.iconSmall)
                                        HorizontalSpacer(Dimens.space1)
                                        Text("Restore", maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            LazyVerticalScroll(modifier = Modifier.weight(1f)) {
                // do not specify a key to avoid jumps when sort/filter is changed
                itemsIndexed(items = visibleEvents) { index, logAndEvent ->
                    EventItem(log = logAndEvent.log, event = logAndEvent.event, display = annotatedDisplay)

                    if (index != visibleEvents.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> EventItem(log: Log<T>, event: Log.Event<T>, display: LogEventDisplay<T>) {
    val content = display.content(event)
    val hasContent = !content.isNullOrBlank()

    val expandedState = if (hasContent) remember { mutableStateOf(false) } else null
    val expanded = expandedState?.value == true

    ContextMenuArea(
        enabled = hasContent,
        items = {
            listOf(
                MaterialContextMenuItem(
                    label = "Copy contents to clipboard",
                    onClick = { setClipboard(contents = requireNotNull(event.content)) },
                    leadingIcon = ContextMenuIcon.OfPainterResource("content-copy.svg"),
                ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = hasContent) { expandedState?.value = !expanded }
                .padding(Dimens.space2)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.space1),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space1), modifier = Modifier.weight(1f)) {
                    display.Icon(event, Modifier.size(Dimens.iconSmall).align(Alignment.Top))

                    Text(
                        text = display.title(log, event),
                        fontFamily = KotifyTypography.Monospace,
                    )
                }

                if (hasContent) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall),
                    )
                }
            }

            if (expanded) {
                Text(
                    text = requireNotNull(content),
                    fontFamily = KotifyTypography.Monospace,
                    modifier = Modifier.padding(start = Dimens.space3),
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth().padding(Dimens.space2),
            ) {
                val textStyle = MaterialTheme.typography.caption.copy(fontFamily = KotifyTypography.Monospace)

                val duration = event.duration
                if (duration == null) {
                    Box(Modifier) // empty element to keep time aligned right
                } else {
                    Text(text = duration.formatShortDuration(decimals = 1), style = textStyle, maxLines = 1)
                }

                Text(text = liveEventTimeText(event.time), style = textStyle, maxLines = 1)
            }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd")
private val timeFormat = DateTimeFormatter.ofPattern("hh:mm:ss.SSS a")

@Composable
private fun liveEventTimeText(timestamp: Long): String {
    val eventDate = remember(timestamp) { Instant.ofEpochMilli(timestamp).atZone(CurrentTime.zoneId) }
    val eventDateString = remember(timestamp) { dateFormat.format(eventDate) }
    val eventTimeString = remember(timestamp) { timeFormat.format(eventDate) }

    return liveRelativeDateText(timestamp = timestamp) { relativeString ->
        buildString {
            if (eventDate.toLocalDate() != LocalDate.now()) append("$eventDateString ")
            append("$eventTimeString ($relativeString)")
        }
    }
}

private class EventTimeProperty<T> : SortableProperty<Log.Event<T>> {
    override val title = "Time"
    override val defaultSortOrder = SortOrder.DESCENDING
    override val terminalSort = true

    override fun compare(sortOrder: SortOrder, first: Log.Event<T>, second: Log.Event<T>): Int {
        return sortOrder.compareBy(first, second) { it.time }
    }
}

private class EventDurationProperty<T> : SortableProperty<Log.Event<T>> {
    override val title = "Duration"
    override val defaultSortOrder = SortOrder.DESCENDING
    override val terminalSort = true

    override fun compare(sortOrder: SortOrder, first: Log.Event<T>, second: Log.Event<T>): Int {
        return sortOrder.compareByNullable(first, second) { it.duration }
    }
}
