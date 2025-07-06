package com.dzirbel.kotify.ui.components.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.theme.KotifyTypography
import com.dzirbel.screenshot.screenshotTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SidePanelTest {
    @ParameterizedTest
    @EnumSource(PanelDirection::class)
    fun testFixed(direction: PanelDirection) {
        val name = direction.name.lowercase()
        screenshotTest(filename = "fixed-$name", defaultFontFamily = KotifyTypography.Default) {
            SidePanel(
                direction = direction,
                panelSize = PanelSize(initialSize = FixedOrPercent.Fixed(200.dp)),
                panelContent = { TestPanel("Panel", Color.DarkGray) },
                mainContent = { TestPanel("Main content", Color.Black) },
            )
        }
    }

    @ParameterizedTest
    @EnumSource(PanelDirection::class)
    fun testPercent(direction: PanelDirection) {
        val name = direction.name.lowercase()
        screenshotTest(filename = "percent-$name", defaultFontFamily = KotifyTypography.Default) {
            SidePanel(
                direction = direction,
                panelSize = PanelSize(initialSize = FixedOrPercent.Percent(0.25f)),
                panelContent = { TestPanel("Panel", Color.DarkGray) },
                mainContent = { TestPanel("Main content", Color.Black) },
            )
        }
    }

    @Composable
    private fun TestPanel(name: String, color: Color) {
        Box(Modifier.fillMaxSize().background(color)) {
            Text(text = name, modifier = Modifier.align(Alignment.Center), color = Color.White)
        }
    }
}
