package com.dzirbel.kotify.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.dzirbel.screenshot.click
import com.dzirbel.screenshot.hover
import com.dzirbel.screenshot.screenshotTest
import org.junit.jupiter.api.Test

class LinkedTextTest {
    @Test
    fun `basic link`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "basic",
            windowWidth = 150,
            windowHeight = 60,
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                text("This is a ")
                link("link", "https://example.com")
                text(" and this is not")
            }
        }

        assertThat(clicks).isEmpty()
    }

    @Test
    fun `hovered link`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "hovered",
            windowWidth = 150,
            windowHeight = 60,
            setUpComposeScene = {
                hover(x = 80f, y = 10f)
            },
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                text("This is a ")
                link("link", "https://example.com")
                text(" and this is not")
            }
        }

        assertThat(clicks).isEmpty()
    }

    @Test
    fun `unhovered link`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "unhovered",
            windowWidth = 150,
            windowHeight = 60,
            setUpComposeScene = {
                hover(x = 20f, y = 10f)
            },
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                text("This is a ")
                link("link", "https://example.com")
                text(" and this is not")
            }
        }

        assertThat(clicks).isEmpty()
    }

    @Test
    fun `clicked link`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "clicked-link",
            windowWidth = 150,
            windowHeight = 60,
            setUpComposeScene = {
                click(x = 70f, y = 10f)
            },
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                text("This is a ")
                link("link", "https://example.com")
                text(" and this is not")
            }
        }

        assertThat(clicks).containsExactly("https://example.com")
    }

    @Test
    fun `clicked text`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "clicked-text",
            windowWidth = 150,
            windowHeight = 60,
            setUpComposeScene = {
                click(x = 20f, y = 10f)
            },
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                text("This is a ")
                link("link", "https://example.com")
                text(" and this is not")
            }
        }

        assertThat(clicks).isEmpty()
    }

    @Test
    fun `text only, unhovered`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "text-only-unhovered",
            windowWidth = 150,
            windowHeight = 60,
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                text("Text with ")
                text("no links")
            }
        }

        assertThat(clicks).isEmpty()
    }

    @Test
    fun `text only, hovered`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "text-only-hovered",
            windowWidth = 150,
            windowHeight = 60,
            setUpComposeScene = {
                hover(x = 40f, y = 10f)
            },
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                text("Text with ")
                text("no links")
            }
        }

        assertThat(clicks).isEmpty()
    }

    @Test
    fun `link only, unhovered`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "link-only-unhovered",
            windowWidth = 150,
            windowHeight = 60,
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                link("Link", "https://example.com")
            }
        }

        assertThat(clicks).isEmpty()
    }

    @Test
    fun `link only, hovered`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "link-only-hovered",
            windowWidth = 150,
            windowHeight = 60,
            setUpComposeScene = {
                hover(x = 20f, y = 10f)
            },
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                link("Link", "https://example.com")
            }
        }

        assertThat(clicks).isEmpty()
    }

    @Test
    fun `link only, clicked`() {
        val clicks = mutableListOf<String>()
        screenshotTest(
            filename = "link-only-clicked",
            windowWidth = 150,
            windowHeight = 60,
            setUpComposeScene = {
                click(x = 20f, y = 10f)
            },
        ) {
            LinkedText(
                onClickLink = clicks::add,
                unhoveredSpanStyle = SpanStyle(color = Color.Red),
                hoveredSpanStyle = SpanStyle(color = Color.Green),
                modifier = Modifier.background(Color.White),
            ) {
                link("Link", "https://example.com")
            }
        }

        assertThat(clicks).containsExactly("https://example.com")
    }
}
