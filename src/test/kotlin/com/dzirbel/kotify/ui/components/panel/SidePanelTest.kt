package com.dzirbel.kotify.ui.components.panel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SidePanelTest {
    @ParameterizedTest
    @EnumSource(PanelDirection::class)
    fun testFixed(direction: PanelDirection) {
        screenshotTest(
            filename = "fixed-${direction.name.lowercase()}",
        ) {
            SidePanel(
                direction = direction,
                panelSize = PanelSize(initialSize = FixedOrPercent.Fixed(200.dp)),
                panelContent = {
                    LocalColors.current.withSurface {
                        Box(Modifier.fillMaxSize().surfaceBackground()) {
                            Text("Panel", modifier = Modifier.align(Alignment.Center))
                        }
                    }
                },
                mainContent = {
                    Box(Modifier.fillMaxSize()) {
                        Text("Main content", modifier = Modifier.align(Alignment.Center))
                    }
                },
            )
        }
    }

    @ParameterizedTest
    @EnumSource(PanelDirection::class)
    fun testPercent(direction: PanelDirection) {
        screenshotTest(
            filename = "percent-${direction.name.lowercase()}",
        ) {
            SidePanel(
                direction = direction,
                panelSize = PanelSize(initialSize = FixedOrPercent.Percent(0.25f)),
                panelContent = {
                    LocalColors.current.withSurface {
                        Box(Modifier.fillMaxSize().surfaceBackground()) {
                            Text("Panel", modifier = Modifier.align(Alignment.Center))
                        }
                    }
                },
                mainContent = {
                    Box(Modifier.fillMaxSize()) {
                        Text("Main content", modifier = Modifier.align(Alignment.Center))
                    }
                },
            )
        }
    }
}
