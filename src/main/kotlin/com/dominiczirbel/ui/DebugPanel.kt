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
import androidx.compose.material.LocalTextStyle
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
import androidx.compose.runtime.collectAsState
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
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens
import com.dominiczirbel.ui.util.collectAsStateSwitchable
import com.dominiczirbel.util.formatByteSize
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
    val tab = tabState.value

    Column {
        Column(Modifier.fillMaxHeight().weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                DebugTab.values().forEach { tab ->
                    TabButton(tab = tab, state = tabState)
                }
            }

            Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))

            Column(Modifier.fillMaxWidth().background(Colors.current.surface3).padding(Dimens.space3)) {
                when (tab) {
                    DebugTab.NETWORK -> NetworkOptions()
                    DebugTab.CACHE -> CacheOptions()
                    DebugTab.IMAGE_CACHE -> ImageCacheOptions()
                }
            }

            EventList(tab.log)
        }

        Spacer(Modifier.height(Dimens.divider).fillMaxWidth().background(Colors.current.dividerColor))

        SimpleTextButton(
            onClick = { tab.log.clear() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Clear log",
                fontSize = Dimens.fontBody
            )
        }
    }
}

@Composable
private fun RowScope.TabButton(tab: DebugTab, state: MutableState<DebugTab>) {
    SimpleTextButton(
        onClick = { state.value = tab },
        modifier = Modifier.fillMaxWidth().weight(1f),
        backgroundColor = if (state.value == tab) MaterialTheme.colors.primary else Color.Transparent
    ) {
        Text(
            text = tab.tabName,
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
                        fontSize = Dimens.fontBody,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Spacer(Modifier.height(Dimens.space1))

                Text(
                    text = eventTimeFormat.format(Date(event.time)),
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
private fun NetworkOptions() {
    val delay = remember { mutableStateOf(DelayInterceptor.delayMs.toString()) }
    val appliedDelay = remember { mutableStateOf(true) }

    // TODO ideally we might reset to the actual delay value on un-focus
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = delay.value,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            fontSize = Dimens.fontBody
        ),
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
    val size = SpotifyCache.sizeFlow.collectAsState(SpotifyCache.size)
    val sizeOnDisk = SpotifyCache.sizeOnDiskFlow.collectAsState(SpotifyCache.sizeOnDisk)
    val sizeOnDiskFormatted = remember(sizeOnDisk.value) { formatByteSize(sizeOnDisk.value) }

    Text(
        text = "${size.value} cached objects; $sizeOnDiskFormatted on disk",
        fontSize = Dimens.fontBody
    )

    Spacer(Modifier.height(Dimens.space2))

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { SpotifyCache.clear() }
    ) {
        Text(
            text = "Clear cache",
            fontSize = Dimens.fontBody
        )
    }
}

@Composable
private fun ImageCacheOptions() {
    val count = SpotifyImageCache.countFlow.collectAsState(SpotifyImageCache.count)
    val totalSize = SpotifyImageCache.totalSizeFlow.collectAsState(SpotifyImageCache.totalSize)
    val totalSizeFormatted = remember(totalSize.value) { formatByteSize(totalSize.value.toLong()) }

    Text(
        text = "${count.value} cached images; $totalSizeFormatted on disk",
        fontSize = Dimens.fontBody
    )

    Spacer(Modifier.height(Dimens.space2))

    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { SpotifyImageCache.clear() }
    ) {
        Text(
            text = "Clear image cache",
            fontSize = Dimens.fontBody
        )
    }
}
