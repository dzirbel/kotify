package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.Logger
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.Theme
import com.dzirbel.kotify.ui.components.HorizontalDivider
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.panel.FixedOrPercent
import com.dzirbel.kotify.ui.components.panel.PanelDirection
import com.dzirbel.kotify.ui.components.panel.PanelSize
import com.dzirbel.kotify.ui.components.panel.SidePanel
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground

enum class DebugTab(val tabName: String, val log: Logger<*>?) {
    EVENTS("Events", null),
    NETWORK("Network", null),
    DATABASE("Database", Logger.Database),
    REPOSITORY("Repository", null),
    IMAGE_CACHE("Images", Logger.ImageCache),
}

private val debugPanelSize = PanelSize(
    initialSize = FixedOrPercent.Fixed(600.dp),
    minPanelSizeDp = 300.dp,
    minContentSizePercent = 0.5f,
)

/**
 * Wraps the debug panel in either a separate window or side panel if open, according to [Settings], and displays the
 * main [content].
 */
@Composable
fun DebugPanel(content: @Composable () -> Unit) {
    val tab = remember { mutableStateOf(DebugTab.entries.first()) }
    val scrollStates = remember { DebugTab.entries.associateWith { ScrollState(0) } }
    val scrollState = scrollStates.getValue(tab.value)

    SidePanel(
        direction = PanelDirection.RIGHT,
        panelSize = debugPanelSize,
        panelEnabled = Settings.debugPanelOpen && !Settings.debugPanelDetached,
        panelContent = {
            DebugPanelContent(tab = tab.value, scrollState = scrollState, onClickTab = { tab.value = it })
        },
        mainContent = content,
    )

    if (Settings.debugPanelOpen && Settings.debugPanelDetached) {
        Window(
            title = "${Application.name} debug tools",
            state = rememberWindowState(placement = WindowPlacement.Maximized),
            onCloseRequest = {
                Settings.debugPanelOpen = false
            },
        ) {
            Theme.Apply {
                DebugPanelContent(tab = tab.value, scrollState = scrollState, onClickTab = { tab.value = it })
            }
        }
    }
}

@Composable
private fun DebugPanelContent(tab: DebugTab, scrollState: ScrollState, onClickTab: (DebugTab) -> Unit) {
    Column(modifier = Modifier.surfaceBackground()) {
        Column(Modifier.fillMaxHeight().weight(1f)) {
            Row(Modifier.fillMaxWidth()) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = {
                        Settings.debugPanelDetached = !Settings.debugPanelDetached
                    },
                ) {
                    val detached = Settings.debugPanelDetached
                    CachedIcon(
                        name = if (detached) "view-sidebar" else "open-in-new",
                        modifier = Modifier.padding(horizontal = Dimens.space3),
                        size = Dimens.iconSmall,
                        contentDescription = if (detached) "Attach to sidebar" else "Open in new window",
                    )
                }

                DebugTab.entries.forEach { buttonTab ->
                    SimpleTextButton(
                        onClick = { onClickTab(buttonTab) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = Dimens.space3, horizontal = Dimens.space1),
                        backgroundColor = if (tab == buttonTab) {
                            LocalColors.current.primary
                        } else {
                            Color.Transparent
                        },
                    ) {
                        Text(buttonTab.tabName, maxLines = 1)
                    }
                }
            }

            HorizontalDivider()

            when (tab) {
                DebugTab.EVENTS -> EventsTab()
                DebugTab.NETWORK -> NetworkTab()
                DebugTab.DATABASE -> DatabaseTab(scrollState)
                DebugTab.REPOSITORY -> RepositoryTab()
                DebugTab.IMAGE_CACHE -> ImageCacheTab(scrollState)
            }
        }

        if (tab.log != null) {
            HorizontalDivider()

            SimpleTextButton(
                onClick = { tab.log.clear() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear log")
            }
        }
    }
}
