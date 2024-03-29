package com.dzirbel.kotify.ui.panel.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import com.dzirbel.kotify.Application
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.ui.CachedIcon
import com.dzirbel.kotify.ui.components.SimpleTextButton
import com.dzirbel.kotify.ui.components.panel.FixedOrPercent
import com.dzirbel.kotify.ui.components.panel.PanelDirection
import com.dzirbel.kotify.ui.components.panel.PanelSize
import com.dzirbel.kotify.ui.components.panel.SidePanel
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.KotifyTheme

private enum class DebugTab(val tabName: String) {
    EVENTS("Events"),
    NETWORK("Network"),
    DATABASE("Database"),
    REPOSITORY("Repository"),
    IMAGE_CACHE("Images"),
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

    SidePanel(
        direction = PanelDirection.RIGHT,
        panelSize = debugPanelSize,
        panelEnabled = Settings.debugPanelOpen && !Settings.debugPanelDetached,
        panelContent = {
            DebugPanelContent(tab = tab.value, onClickTab = { tab.value = it })
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
            KotifyTheme.Apply(
                colors = Settings.colors,
                instrumentationHighlightCompositions = Settings.instrumentationHighlightCompositions,
                instrumentationMetricsPanels = Settings.instrumentationMetricsPanels,
            ) {
                DebugPanelContent(tab = tab.value, onClickTab = { tab.value = it })
            }
        }
    }
}

@Composable
private fun DebugPanelContent(tab: DebugTab, onClickTab: (DebugTab) -> Unit) {
    Surface(elevation = Dimens.panelElevationSmall) {
        Column(Modifier.fillMaxHeight()) {
            Surface(elevation = Dimens.componentElevation) {
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
                            colors = if (tab == buttonTab) {
                                ButtonDefaults.textButtonColors(
                                    backgroundColor = MaterialTheme.colors.primary,
                                    contentColor = MaterialTheme.colors.onPrimary,
                                )
                            } else {
                                ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onBackground)
                            },
                        ) {
                            Text(buttonTab.tabName, maxLines = 1)
                        }
                    }
                }
            }

            when (tab) {
                DebugTab.EVENTS -> EventsTab()
                DebugTab.NETWORK -> NetworkTab()
                DebugTab.DATABASE -> DatabaseTab()
                DebugTab.REPOSITORY -> RepositoryTab()
                DebugTab.IMAGE_CACHE -> ImageCacheTab()
            }
        }
    }
}
