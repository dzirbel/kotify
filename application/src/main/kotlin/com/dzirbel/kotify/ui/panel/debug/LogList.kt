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
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dzirbel.contextmenu.ContextMenuIcon
import com.dzirbel.contextmenu.MaterialContextMenuItem
import com.dzirbel.kotify.log.FlowView
import com.dzirbel.kotify.log.Log
import com.dzirbel.kotify.ui.components.HorizontalDivider
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
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import com.dzirbel.kotify.ui.util.setClipboard
import com.dzirbel.kotify.util.CurrentTime
import com.dzirbel.kotify.util.capitalize
import com.dzirbel.kotify.util.collections.mapLazy
import com.dzirbel.kotify.util.coroutines.mapIn
import com.dzirbel.kotify.util.time.formatShortDuration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface LogEventDisplay<T> {
    val Log.Event.Type.color: Color
        @Composable
        get() {
            return when (this) {
                Log.Event.Type.INFO -> LocalColors.current.text
                Log.Event.Type.SUCCESS -> Color.Green
                Log.Event.Type.WARNING -> Color.Yellow
                Log.Event.Type.ERROR -> LocalColors.current.error
            }
        }

    fun title(log: Log<T>, event: Log.Event<T>): String = event.title

    fun hasContent(event: Log.Event<T>): Boolean = !event.content.isNullOrBlank()

    fun content(event: Log.Event<T>): String? = event.content

    @Composable
    fun Icon(event: Log.Event<T>, modifier: Modifier) {
        Icon(
            imageVector = when (event.type) {
                Log.Event.Type.INFO -> Icons.Default.Info
                Log.Event.Type.SUCCESS -> Icons.Default.Check
                Log.Event.Type.WARNING -> Icons.Default.Warning
                Log.Event.Type.ERROR -> Icons.Default.Close
            },
            contentDescription = null,
            modifier = modifier,
            tint = event.type.color,
        )
    }
}

data class LogAndEvent<T>(val log: Log<T>, val event: Log.Event<T>)

// TODO add separators for page changes
@Composable
fun <T> LogList(
    logs: Iterable<Log<T>>,
    display: LogEventDisplay<T> = object : LogEventDisplay<T> {},
    sortProperties: ImmutableList<SortableProperty<Log.Event<T>>> = persistentListOf(),
    filter: ((LogAndEvent<T>) -> Boolean)? = null,
    annotateTitlesByLog: Boolean = logs.count() > 1,
    headerContent: @Composable (eventsFlow: StateFlow<ImmutableList<LogAndEvent<T>>>) -> Unit,
) {
    LocalColors.current.WithSurface {
        Column {
            val selectedEventTypes = remember { mutableStateOf(persistentSetOf<Log.Event.Type>()) }

            val sortableProperties = remember(sortProperties) {
                persistentListOf<SortableProperty<Log.Event<T>>>(EventTimeProperty(), EventDurationProperty())
                    .addAll(sortProperties)
            }
            val sorts = remember { mutableStateOf(persistentListOf(Sort(sortableProperties.first()))) }
            val clearTime = remember { mutableStateOf<Long?>(null) }

            val scope = rememberCoroutineScope()

            // TODO atrocious
            val eventsFlow = remember(logs, clearTime.value, sorts.value) {
                FlowView<LogAndEvent<T>>(
                    sort = Comparator.comparing({ it.event }, sorts.value.asComparator()),
                    filter = clearTime.value?.let { clearTime -> { (_, event) -> event.time > clearTime } },
                )
                    .viewState(
                        flow = logs
                            .mapLazy { log ->
                                log.eventsFlow.map { event -> LogAndEvent(log, event) }
                            }
                            .merge(),
                        initial = logs.flatMap { log ->
                            log.events.mapLazy { event -> LogAndEvent(log, event) }
                        },
                        scope = scope,
                    )
            }

            val (countsByType, visibleEvents, numEvents) = remember(eventsFlow, selectedEventTypes.value, filter) {
                // do not filter if all or none are selected (no-op filter)
                val filterEventType = selectedEventTypes.value.size in 1 until Log.Event.Type.entries.size

                eventsFlow.mapIn(scope) { events ->
                    val countsByType = IntArray(Log.Event.Type.entries.size)
                    val visibleEvents = mutableListOf<LogAndEvent<T>>()
                    for (logAndEvent in events) {
                        val event = logAndEvent.event
                        countsByType[event.type.ordinal]++
                        if ((!filterEventType || selectedEventTypes.value.contains(event.type)) &&
                            filter?.invoke(logAndEvent) != false
                        ) {
                            visibleEvents.add(logAndEvent)
                        }
                    }

                    Triple(countsByType, visibleEvents, events.size)
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

            Column(Modifier.surfaceBackground()) {
                headerContent(eventsFlow)

                HorizontalDivider()

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
                                val count = countsByType[type.ordinal]
                                val name = type.name.lowercase().capitalize()
                                Text("$name [$count]")
                            },
                        )

                        Text("${visibleEvents.size}/$numEvents visible")
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

                        SimpleTextButton(onClick = { clearTime.value = CurrentTime.millis }) {
                            Text("Clear")
                        }
                    }
                }

                HorizontalDivider()
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
    val hasContent = display.hasContent(event)

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
                    text = display.content(event).orEmpty(),
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
                    Text(text = duration.formatShortDuration(decimals = 1), style = textStyle)
                }

                Text(text = liveEventTimeText(event.time), style = textStyle)
            }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd")
private val timeFormat = DateTimeFormatter.ofPattern("hh:mm:ss.SSS a")

@Composable
private fun liveEventTimeText(timestamp: Long): String {
    val eventDate = remember(timestamp) { Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()) }
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
