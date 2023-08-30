package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.key
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
import com.dzirbel.kotify.ui.components.VerticalScroll
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyTypography
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.setClipboard
import com.dzirbel.kotify.util.collections.mapLazy
import com.dzirbel.kotify.util.formatDateTime
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

@Composable
fun <T> LogList(
    logs: Iterable<Log<T>>,
    modifier: Modifier = Modifier,
    view: FlowView<Log.Event<T>> = FlowView(),
    annotateTitlesByLog: Boolean = logs.count() > 1,
    scrollState: ScrollState = rememberScrollState(0),
) {
    // TODO use LazyColumn
    VerticalScroll(modifier = modifier, scrollState = scrollState) {
        val scope = rememberCoroutineScope()

        val events: ImmutableList<Pair<Log<T>, Log.Event<T>>> = remember(logs, view) {
            // annotate each event with the log it came from
            view
                .transformed<Pair<Log<T>, Log.Event<T>>> { (_, event) -> event }
                .viewState(
                    flow = logs
                        .mapLazy { log -> log.eventsFlow.map { event -> Pair(log, event) } }
                        .merge(),
                    initial = logs.flatMap { log -> log.events.mapLazy { event -> Pair(log, event) } },
                    scope = scope,
                )
        }
            .collectAsState()
            .value

        events.forEachIndexed { index, (log, event) ->
            key(event) {
                EventItem(
                    event = event,
                    eventTitle = if (annotateTitlesByLog) "[${log.name}] ${event.title}" else event.title,
                )
            }

            if (index != events.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

// TODO add optional custom content based on T
@Composable
private fun <T> EventItem(event: Log.Event<T>, eventTitle: String = event.title) {
    val canExpand = !event.content.isNullOrBlank()

    val expandedState = if (canExpand) remember { mutableStateOf(false) } else null
    val expanded = expandedState?.value == true

    ContextMenuArea(
        enabled = canExpand,
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
                .clickable(enabled = canExpand) { expandedState?.value = !expanded }
                .padding(Dimens.space2)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.space1),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space1), modifier = Modifier.weight(1f)) {
                    // TODO add icons for in-memory, database, remote, etc
                    Icon(
                        imageVector = when (event.type) {
                            Log.Event.Type.INFO -> Icons.Default.Info
                            Log.Event.Type.SUCCESS -> Icons.Default.Check
                            Log.Event.Type.WARNING -> Icons.Default.Warning
                            Log.Event.Type.ERROR -> Icons.Default.Close
                        },
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall).align(Alignment.Top),
                        tint = when (event.type) {
                            Log.Event.Type.INFO -> LocalColors.current.text
                            Log.Event.Type.SUCCESS -> Color.Green
                            Log.Event.Type.WARNING -> Color.Yellow
                            Log.Event.Type.ERROR -> LocalColors.current.error
                        },
                    )

                    Text(
                        text = eventTitle,
                        fontFamily = KotifyTypography.Monospace,
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
                    fontFamily = KotifyTypography.Monospace,
                    modifier = Modifier.padding(start = Dimens.space3),
                )
            }

            Text(
                text = remember(event.time) { formatDateTime(event.time, includeMillis = true) },
                style = MaterialTheme.typography.overline.copy(fontFamily = KotifyTypography.Monospace),
                modifier = Modifier.padding(Dimens.space2).align(Alignment.End),
            )
        }
    }
}
