package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dominiczirbel.ui.theme.Colors
import com.dominiczirbel.ui.theme.Dimens

@Suppress("MagicNumber")
@Composable
fun MainContent() {
    val leftPanelState = remember { PanelState(initialSize = 500.dp, minSize = 200.dp, minContentSize = 500.dp) }

    Column(Modifier.fillMaxSize()) {
        SidePanel(
            modifier = Modifier.fillMaxSize().weight(1f),
            direction = PanelDirection.LEFT,
            state = leftPanelState,
            panelContent = { LibraryPanel() },
            mainContent = {
                Box(modifier = Modifier.padding(Dimens.space3)) {
                    Text("Content", color = Colors.current.text, fontSize = Dimens.fontTitle)
                }
            }
        )

        BottomPanel()
    }
}
