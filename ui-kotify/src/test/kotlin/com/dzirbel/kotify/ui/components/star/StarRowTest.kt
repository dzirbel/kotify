package com.dzirbel.kotify.ui.components.star

import androidx.compose.ui.ComposeScene
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.unit.dp
import com.dzirbel.kotify.ui.themedScreenshotTest
import org.junit.jupiter.api.Test

class StarRowTest {
    @Test
    fun testBase() {
        val rating = 3.5
        themedScreenshotTest(filename = "stars-base", windowWidth = WINDOW_WIDTH, windowHeight = WINDOW_HEIGHT) {
            StarRow(getStarRating = { rating }, stars = MAX_RATING, starSize = STAR_SIZE.dp)
        }
    }

    @Test
    fun testHoverMore() {
        val rating = 1
        val hoverStar = 3.5f // hover the 4th star
        themedScreenshotTest(
            filename = "stars-hover-more",
            windowWidth = WINDOW_WIDTH,
            windowHeight = WINDOW_HEIGHT,
            setUpComposeScene = {
                hover(x = hoverStar * STAR_SIZE, y = STAR_SIZE / 2f)
            },
        ) {
            StarRow(getStarRating = { rating }, stars = MAX_RATING, starSize = STAR_SIZE.dp)
        }
    }

    @Test
    fun testHoverLess() {
        val rating = 4
        val hoverStar = 1.5f // hover the 2nd star
        themedScreenshotTest(
            filename = "stars-hover-less",
            windowWidth = WINDOW_WIDTH,
            windowHeight = WINDOW_HEIGHT,
            setUpComposeScene = {
                hover(x = hoverStar * STAR_SIZE, y = STAR_SIZE / 2f)
            },
        ) {
            StarRow(getStarRating = { rating }, stars = MAX_RATING, starSize = STAR_SIZE.dp)
        }
    }

    companion object {
        private const val STAR_SIZE = 25
        private const val MAX_RATING = 5
        private const val WINDOW_WIDTH = MAX_RATING * STAR_SIZE
        private const val WINDOW_HEIGHT = STAR_SIZE

        private fun ImageComposeScene.hover(x: Float, y: Float) {
            sendPointerEvent(
                eventType = PointerEventType.Move,
                pointers = listOf(
                    ComposeScene.Pointer(id = PointerId(0L), position = Offset(x = x, y = y), pressed = false),
                ),
            )
        }
    }
}
