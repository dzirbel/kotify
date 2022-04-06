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
import com.dzirbel.kotify.ui.components.adapter.ListAdapter
import com.dzirbel.kotify.ui.components.adapter.SortOrder
import com.dzirbel.kotify.ui.properties.PropertyByNumber
import com.dzirbel.kotify.ui.properties.PropertyByString
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
                Table(columns = columns, items = ListAdapter.of(items), onSetSort = {})
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
                        object : PropertyByString<String>(title = "Col 1") {
                            override fun toString(item: String) = item
                        },
                    ),
                    items = listOf("First", "Second", "Third"),
                ),

                ScreenshotTestCase(
                    filename = "medium",
                    columns = listOf(
                        object : ColumnByNumber<IndexedValue<String>> {
                            override val title = "#"

                            override fun toNumber(item: IndexedValue<String>) = item.index + 1
                        },

                        object : PropertyByString<IndexedValue<String>>(title = "Col 1") {
                            override fun toString(item: IndexedValue<String>) = item.value
                        },

                        object : Column<IndexedValue<String>> {
                            override val title = "Col 2"
                            override val width = ColumnWidth.Weighted(weight = 1f)
                            override val cellAlignment = Alignment.BottomEnd

                            @Composable
                            override fun item(item: IndexedValue<String>) {
                                val color = when {
                                    item.index % 3 == 0 -> Color.Red
                                    item.index % 3 == 1 -> Color.Green
                                    item.index % 3 == 2 -> Color.Blue
                                    else -> error("impossible")
                                }

                                Box(Modifier.background(color)) {
                                    Text("${item.value} - ${20 - item.index} from the end")
                                }
                            }
                        },
                    ),
                    items = List(20) { IndexedValue(index = it, value = "${it}th item") },
                ),

                ScreenshotTestCase(
                    filename = "complex",
                    windowHeight = 7_000,
                    columns = listOf(
                        object : ColumnByNumber<Int> {
                            override val title = "#"

                            override fun toNumber(item: Int) = item + 1
                        },

                        object : Column<Int> {
                            override val title = "Unused"
                            override val width = ColumnWidth.Fixed(50.dp)
                            override val cellAlignment = Alignment.Center
                            override val headerAlignment = Alignment.Center

                            @Composable
                            override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
                                Text("Custom header")
                            }

                            @Composable
                            override fun item(item: Int) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.Yellow,
                                )
                            }
                        },

                        object : Column<Int> {
                            override val title = "Unused"
                            override val width = ColumnWidth.MatchHeader
                            override val cellAlignment = Alignment.Center
                            override val headerAlignment = Alignment.Center

                            @Composable
                            override fun header(sortOrder: SortOrder?, onSetSort: (SortOrder?) -> Unit) {
                                Text("H", modifier = Modifier.padding(Dimens.space1))
                            }

                            @Composable
                            override fun item(item: Int) {
                                Box(Modifier.fillMaxWidth().height(20.dp).background(Color.Gray))
                            }
                        },

                        object : PropertyByString<Int>(title = "Even or odd?") {
                            override val width = ColumnWidth.Weighted(weight = 1.5f)

                            override fun toString(item: Int): String {
                                return if (item % 2 == 0) "Even" else "Odd"
                            }
                        },

                        object : PropertyByNumber<Int>(title = "Collatz", divisionRange = 10) {
                            override val width = ColumnWidth.Weighted(weight = 1.5f)

                            override fun toNumber(item: Int): Number {
                                return if (item % 2 == 0) item / 2 else 3 * item + 1
                            }
                        },
                    ),
                    items = List(200) { it },
                ),
            )
        }
    }
}
