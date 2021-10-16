package com.dzirbel.kotify.ui.components.table

import androidx.compose.runtime.Composable
import com.dzirbel.kotify.ui.screenshotTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class TableTest {
    data class ScreenshotTestCase<T>(val filename: String, val columns: List<Column<T>>, val items: List<T>)

    @ParameterizedTest
    @MethodSource("screenshotTestCases")
    fun test(testCase: ScreenshotTestCase<*>) {
        screenshotTest(filename = testCase.filename) {
            @Composable
            fun <T> ScreenshotTestCase<T>.Table() {
                Table(columns = columns, items = items)
            }

            testCase.Table()
        }
    }

    companion object {
        @JvmStatic
        fun screenshotTestCases(): List<ScreenshotTestCase<*>> {
            return listOf(
                ScreenshotTestCase(
                    filename = "simple",
                    columns = listOf(
                        object : ColumnByString<String>(header = "Col 1") {
                            override fun toString(item: String, index: Int) = item
                        }
                    ),
                    items = listOf("First", "Second", "Third")
                )
            )
        }
    }
}
