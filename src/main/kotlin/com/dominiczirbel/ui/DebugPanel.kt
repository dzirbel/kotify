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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.collectAsStateSwitchable
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date

private enum class DebugTab(val tabName: String, val log: Logger) {
    NETWORK("Network", Logger.Network),
    CACHE("Cache", Logger.Cache),
    IMAGE_CACHE("Image Cache", Logger.ImageCache)
}

private val eventTimeFormat = SimpleDateFormat("HH:mm:ss.SSSS")
private const val MAX_EVENTS = 500

@Composable
fun DebugPanel() {
    val tabState = remember { mutableStateOf(DebugTab.values().first()) }

    Column {
        Column(Modifier.fillMaxHeight().weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                DebugTab.values().forEach { tab ->
                    TabButton(tab = tab, state = tabState)
                }
            }

            Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))

            EventList(tabState.value.log)
        }

        Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))

        TextButton(
            onClick = { tabState.value.log.clear() },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0)
        ) {
            Text(
                text = "Clear log",
                color = Colors.current.text,
                fontSize = Dimens.fontBody
            )
        }
    }
}

@Composable
private fun RowScope.TabButton(tab: DebugTab, state: MutableState<DebugTab>) {
    TextButton(
        onClick = { state.value = tab },
        modifier = Modifier.fillMaxWidth().weight(1f),
        shape = RoundedCornerShape(0),
        colors = if (state.value == tab) {
            ButtonDefaults.textButtonColors(
                backgroundColor = MaterialTheme.colors.primary
            )
        } else {
            ButtonDefaults.textButtonColors()
        }
    ) {
        Text(
            text = tab.tabName,
            color = Colors.current.text,
            fontSize = Dimens.fontBody
        )
    }
}

@Composable
private fun EventList(log: Logger) {
    val events = log.events()
        .map { it.reversed().take(MAX_EVENTS) }
        .collectAsStateSwitchable(initial = { log.events.reversed().take(MAX_EVENTS) }, key = log)

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
                            Logger.Event.Type.ERROR -> Color.Red
                        }
                    )

                    Spacer(Modifier.width(Dimens.space1))

                    Text(
                        text = event.message,
                        color = Colors.current.text,
                        fontSize = Dimens.fontBody,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Spacer(Modifier.height(Dimens.space1))

                Text(
                    text = eventTimeFormat.format(Date(event.time)),
                    color = Colors.current.text,
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
