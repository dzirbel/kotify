package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Dimens
import com.dzirbel.kotify.ui.theme.LocalColors
import com.dzirbel.kotify.ui.theme.surfaceBackground
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class FlowTest {
    data class ScreenshotTestCase(
        val filename: String,
        val elements: Int,
        val alignment: Alignment.Vertical = Alignment.Top,
        val verticalPadding: (Int) -> Dp = { 0.dp },
    )

    private val colorsSet = setOf(Colors.DARK)

    @Test
    fun testScreenshotEmpty() {
        screenshotTest(filename = "empty", windowWidth = 100, windowHeight = 50, colorsSet = colorsSet) {
            Flow { }
        }
    }

    @ParameterizedTest
    @MethodSource("screenshotTestCases")
    fun testScreenshot(case: ScreenshotTestCase) {
        screenshotTest(filename = case.filename, windowWidth = 500, windowHeight = 250, colorsSet = colorsSet) {
            Flow(
                modifier = Modifier.padding(Dimens.space3),
                verticalAlignment = case.alignment,
            ) {
                List(case.elements) { index ->
                    LocalColors.current.WithSurface(increment = Colors.INCREMENT_LARGE) {
                        Text(
                            text = "element ${index + 1}",
                            modifier = Modifier
                                .surfaceBackground()
                                .padding(
                                    horizontal = Dimens.space2,
                                    vertical = Dimens.space2 + case.verticalPadding(index),
                                ),
                        )
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Suppress("UnnecessaryParentheses")
        fun screenshotTestCases(): List<ScreenshotTestCase> {
            return listOf(
                ScreenshotTestCase(filename = "single-row", elements = 3),
                ScreenshotTestCase(filename = "multiple-rows-simple", elements = 20),
                ScreenshotTestCase(
                    filename = "multiple-rows-varied-top",
                    elements = 20,
                    alignment = Alignment.Top,
                    verticalPadding = { ((it % 5) * 2).dp },
                ),
                ScreenshotTestCase(
                    filename = "multiple-rows-varied-center",
                    elements = 20,
                    alignment = Alignment.CenterVertically,
                    verticalPadding = { ((it % 5) * 2).dp },
                ),
                ScreenshotTestCase(
                    filename = "multiple-rows-varied-bottom",
                    elements = 20,
                    alignment = Alignment.Bottom,
                    verticalPadding = { ((it % 5) * 2).dp },
                ),
                ScreenshotTestCase(filename = "overflow", elements = 50),
            )
        }
    }
}
