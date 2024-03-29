package com.dzirbel.kotify.ui.components.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.components.adapter.DividableProperty
import com.dzirbel.kotify.ui.components.adapter.Divider
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.components.adapter.compare
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.ui.themedScreenshotTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class GridTest {
    data class ScreenshotTestCase(
        val filename: String,
        val alignment: Alignment,
        val columns: Int?,
        val adapter: ListAdapter<Int> = ListAdapter.of(List(50) { it }),
        val selectedElementIndex: Int? = null,
    )

    @ParameterizedTest
    @MethodSource("screenshotTestCases")
    fun testScreenshot(case: ScreenshotTestCase) {
        themedScreenshotTest(filename = case.filename, windowHeight = 1500, colors = listOf(KotifyColors.DARK)) {
            Grid(
                modifier = Modifier,
                elements = case.adapter,
                columns = case.columns,
                horizontalSpacing = 10.dp,
                verticalSpacing = 5.dp,
                edgePadding = PaddingValues(horizontal = 20.dp, vertical = 5.dp),
                cellAlignment = case.alignment,
                selectedElementIndex = case.selectedElementIndex,
                detailInsertContent = { _, element ->
                    Text(
                        text = "Details for $element",
                        modifier = Modifier.padding(24.dp),
                    )
                },
            ) { _, index ->
                Box(modifier = Modifier.background(Color.DarkGray).size((20 + index * 2).dp)) {
                    Text(
                        text = (index + 1).toString(),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }

    companion object {
        private class ModDividableProperty(val mod: Int) : DividableProperty<Int> {
            override val title = "mod $mod divider"

            override fun divisionFor(element: Int) = element % mod

            override fun compareDivisions(sortOrder: SortOrder, first: Any?, second: Any?): Int {
                return sortOrder.compare(requireNotNull(first) as Int, requireNotNull(second) as Int)
            }

            @Composable
            override fun DivisionHeader(division: Any?) {
                Text(
                    text = division.toString(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.background(Color.DarkGray).fillMaxWidth(),
                )
            }
        }

        @JvmStatic
        fun screenshotTestCases(): List<ScreenshotTestCase> {
            return listOf(
                ScreenshotTestCase(filename = "center", alignment = Alignment.Center, columns = 6),
                ScreenshotTestCase(filename = "bottom-end", alignment = Alignment.BottomEnd, columns = 6),
                ScreenshotTestCase(filename = "dynamic-cols", alignment = Alignment.Center, columns = null),
                ScreenshotTestCase(
                    filename = "dividers",
                    alignment = Alignment.Center,
                    columns = null,
                    adapter = ListAdapter.of(List(50) { it })
                        .withDivider(Divider(dividableProperty = ModDividableProperty(mod = 5))),
                ),
                ScreenshotTestCase(
                    filename = "insert",
                    alignment = Alignment.Center,
                    columns = null,
                    selectedElementIndex = 13,
                ),
            )
        }
    }
}
