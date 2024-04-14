package com.dzirbel.kotify.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import com.dzirbel.kotify.Settings
import com.dzirbel.kotify.ui.page.Page
import com.dzirbel.kotify.ui.page.PageStack
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.screenshot.screenshotTest

/**
 * Wrapper around [screenshotTest] which provides setup for taking a screenshot of the entire application state.
 */
fun Any.applicationScreenshotTest(
    filename: String,
    page: Page,
    record: Boolean = false,
    content: @Composable () -> Unit,
) {
    screenshotTest(
        filename = filename,
        configurations = listOf(KotifyColors.DARK, KotifyColors.LIGHT),
        windowWidth = 1920,
        windowHeight = 1080,
        windowDensity = Density(density = 0.65f, fontScale = 1.25f),
        record = record,
        configurationName = { it.name.lowercase() },
        onConfiguration = { colors -> Settings.colors = colors },
    ) {
        pageStack.value = PageStack(page)
        content()
    }
}
