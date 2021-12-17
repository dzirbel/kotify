package com.dzirbel.kotify.ui.components.table

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.screenshotTest
import com.dzirbel.kotify.ui.theme.Dimens
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class TableTest {
    data class ScreenshotTestCase<T>(
        val filename: String,
        val columns: List<Column<T>>,
        val items: List<T>,
        val windowHeight: Int = 768,
    )

    @ParameterizedTest
    @MethodSource("screenshotTestCases")
    fun test(testCase: ScreenshotTestCase<*>) {
        screenshotTest(filename = testCase.filename, windowHeight = testCase.windowHeight) {
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
                ),

                ScreenshotTestCase(
                    filename = "medium",
                    columns = listOf(
                        IndexColumn(),

                        object : ColumnByString<String>(header = "Col 1") {
                            override fun toString(item: String, index: Int) = item
                        },

                        object : Column<String>() {
                            override val width = ColumnWidth.Weighted(weight = 1f)
                            override val cellAlignment = Alignment.BottomEnd

                            @Composable
                            override fun header(sort: Sort?, onSetSort: (Sort?) -> Unit) {
                                standardHeader(sort = sort, onSetSort = onSetSort, header = "Col 2", sortable = false)
                            }

                            @Composable
                            override fun item(item: String, index: Int) {
                                val color = when {
                                    index % 3 == 0 -> Color.Red
                                    index % 3 == 1 -> Color.Green
                                    index % 3 == 2 -> Color.Blue
                                    else -> error("impossible")
                                }

                                Box(Modifier.background(color)) {
                                    Text("$item - ${20 - index} from the end")
                                }
                            }
                        }
                    ),
                    items = List(20) { "${it}th item" }
                ),

                ScreenshotTestCase(
                    filename = "complex",
                    windowHeight = 7_000,
                    columns = listOf(
                        IndexColumn(),

                        object : Column<Int>() {
                            override val width = ColumnWidth.Fixed(50.dp)
                            override val cellAlignment = Alignment.Center
                            override val headerAlignment = Alignment.Center

                            @Composable
                            override fun header(sort: Sort?, onSetSort: (Sort?) -> Unit) {
                                Text("Custom header")
                            }

                            @Composable
                            override fun item(item: Int, index: Int) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                )
                            }
                        },

                        object : Column<Int>() {
                            override val width = ColumnWidth.MatchHeader
                            override val cellAlignment = Alignment.Center
                            override val headerAlignment = Alignment.Center

                            @Composable
                            override fun header(sort: Sort?, onSetSort: (Sort?) -> Unit) {
                                Text("H", modifier = Modifier.padding(Dimens.space1))
                            }

                            @Composable
                            override fun item(item: Int, index: Int) {
                                Box(Modifier.fillMaxWidth().height(20.dp).background(Color.Gray))
                            }
                        },

                        object : ColumnByString<Int>(header = "Even or odd?") {
                            override val width = ColumnWidth.Weighted(weight = 1.5f)

                            override fun toString(item: Int, index: Int): String {
                                return if (item % 2 == 0) "Even" else "Odd"
                            }
                        },

                        object : ColumnByNumber<Int>(header = "Collatz") {
                            override val width = ColumnWidth.Weighted(weight = 1.5f)

                            override fun toNumber(item: Int, index: Int): Number {
                                return if (item % 2 == 0) item / 2 else 3 * item + 1
                            }
                        },
                    ),
                    items = List(200) { it }
                )
            )
        }
    }
}
