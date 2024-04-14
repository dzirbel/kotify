package com.dzirbel.kotify.ui

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.theme.KotifyTheme
import com.dzirbel.screenshot.screenshotTest

fun Any.themedScreenshotTest(
    filename: String,
    windowWidth: Int = 1024,
    windowHeight: Int = 768,
    windowDensity: Density = Density(1f),
    record: Boolean = false,
    colors: List<KotifyColors> = listOf(KotifyColors.DARK, KotifyColors.LIGHT),
    setUpComposeScene: ImageComposeScene.() -> Unit = {},
    onColors: (colors: KotifyColors) -> Unit = {},
    content: @Composable () -> Unit,
) {
    screenshotTest(
        filename = filename,
        configurations = colors,
        windowWidth = windowWidth,
        windowHeight = windowHeight,
        windowDensity = windowDensity,
        record = record,
        setUpComposeScene = setUpComposeScene,
        configurationName = { it.name.lowercase() },
        onConfiguration = onColors,
    ) { runColors ->
        KotifyTheme.Apply(colors = runColors) {
            Surface(content = content)
        }
    }
}
