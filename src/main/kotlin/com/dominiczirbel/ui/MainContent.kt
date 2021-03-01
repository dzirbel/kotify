package com.dominiczirbel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dominiczirbel.ui.constants.Dimens
import com.dominiczirbel.ui.constants.Theme

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
                Text("Content", color = Theme.current.text, fontSize = Dimens.fontTitle)
            }
        )

        BottomPanel()
    }
}
