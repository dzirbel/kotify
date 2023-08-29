package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.VerticalScroll
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyTypography
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.util.applyIf
import com.dzirbel.kotify.ui.util.setClipboard
import com.dzirbel.kotify.util.formatDateTime
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map

private const val MAX_EVENTS = 500

@Composable
fun <T> EventList(
    log: Logger<T>,
    key: Any,
    scrollState: ScrollState = rememberScrollState(0),
    filter: (Logger.Event<T>) -> Boolean = { true },
) {
    VerticalScroll(scrollState = scrollState) {
        val flow: SharedFlow<List<Logger.Event<T>>> = remember(key, filter) { log.eventsFlow }
        val initial = remember(key, filter) { flow.replayCache.firstOrNull().orEmpty().filter(filter).take(MAX_EVENTS) }
        val events: List<Logger.Event<T>> = flow
            .map { it.filter(filter).take(MAX_EVENTS) }
            .collectAsState(initial = initial)
            .value

        events.asReversed().forEachIndexed { index, event ->
            key(event) {
                EventItem(event)
            }

            if (index != events.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun <T> EventItem(event: Logger.Event<T>) {
    val canExpand = !event.content.isNullOrBlank()

    val expandedState = if (canExpand) remember { mutableStateOf(false) } else null
    val expanded = expandedState?.value == true

    val rightClickMenuExpanded = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .applyIf(canExpand) {
                onClick(matcher = PointerMatcher.mouse(PointerButton.Primary)) {
                    expandedState?.value = !expanded
                }
                    .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                        rightClickMenuExpanded.value = true
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
                    },
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
