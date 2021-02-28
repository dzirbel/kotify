package com.dominiczirbel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PADDING = 10.dp

@Suppress("MagicNumber")
@Composable
fun MainContent() {
    val leftPanelState = remember { PanelState(initialSize = 300.dp, minSize = 100.dp, minContentSize = 200.dp) }
    val rightPanelState = remember { PanelState(initialSize = 300.dp, minSize = 100.dp, minContentSize = 200.dp) }
    val topPanelState = remember { PanelState(initialSize = 150.dp, minSize = 50.dp, minContentSize = 200.dp) }
    val bottomPanelState = remember { PanelState(initialSize = 150.dp, minSize = 50.dp, minContentSize = 200.dp) }

    SidePanel(
        direction = PanelDirection.LEFT,
        state = leftPanelState,
        panelContent = {
            Box(modifier = Modifier.fillMaxSize().padding(PADDING).background(Color.Green)) {
                Text("left")
            }
        },
        mainContent = {
            SidePanel(
                direction = PanelDirection.RIGHT,
                state = rightPanelState,
                panelContent = {
                    Text("right", modifier = Modifier.fillMaxSize().padding(PADDING).background(Color.Red))
                },
                mainContent = {
                    SidePanel(
                        direction = PanelDirection.TOP,
                        state = topPanelState,
                        panelContent = {
                            Text("top", modifier = Modifier.fillMaxSize().padding(PADDING).background(Color.Cyan))
                        },
                        mainContent = {
                            SidePanel(
                                direction = PanelDirection.BOTTOM,
                                state = bottomPanelState,
                                panelContent = {
                                    Text(
                                        "bottom",
                                        modifier = Modifier.fillMaxSize().padding(PADDING).background(Color.Yellow)
                                    )
                                },
                                mainContent = {
                                    Text(
                                        "content",
                                        modifier = Modifier.fillMaxSize().padding(PADDING).background(Color.LightGray)
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    )
}
