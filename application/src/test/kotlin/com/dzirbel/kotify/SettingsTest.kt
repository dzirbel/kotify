package com.dzirbel.kotify

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.dzirbel.kotify.ui.theme.KotifyColors
import com.dzirbel.kotify.util.MockedTimeExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockedTimeExtension::class)
class SettingsTest {
    @Test
    fun saveAndLoad() {
        val tempFile = createTempJsonFile("save-and-load")
        val settings = Settings.SettingsData()

        Settings.save(settings, tempFile)

        val loaded = Settings.load(tempFile)

        assertThat(loaded).isEqualTo(settings)
    }

    @Test
    fun saveDefault() {
        val tempFile = createTempJsonFile("save-default")
        val settings = Settings.SettingsData()

        Settings.save(settings, tempFile)

        assertThat(tempFile.readText()).isEqualTo(
            """
                {
                    "colors": "DARK",
                    "debugPanelOpen": false,
                    "debugPanelDetached": false,
                    "instrumentationHighlightCompositions": false,
                    "instrumentationMetricsPanels": false
                }
            """.trimIndent(),
        )
    }

    @Test
    fun loadDefault() {
        val tempFile = createTempJsonFile("load-default") {
            """
                {
                    "colors": "DARK",
                    "debugPanelOpen": false,
                    "debugPanelDetached": false,
                    "instrumentationHighlightCompositions": false,
                    "instrumentationMetricsPanels": false
                }
            """.trimIndent()
        }
        val settings = Settings.SettingsData()

        assertThat(Settings.load(tempFile)).isEqualTo(settings)
    }

    @Test
    fun loadInvalidColors() {
        val tempFile = createTempJsonFile("load-invalid-colors") { """{ "colors": "INVALID" }""" }
        val settings = Settings.SettingsData()
        assertThat(Settings.load(tempFile)).isEqualTo(settings)
    }

    @Test
    fun loadLowercaseColors() {
        val tempFile = createTempJsonFile("load-invalid-colors") { """{ "colors": "light" }""" }
        val settings = Settings.SettingsData(colors = KotifyColors.LIGHT)
        assertThat(Settings.load(tempFile)).isEqualTo(settings)
    }

    @Test
    fun loadUnknownKey() {
        val tempFile = createTempJsonFile("load-unknown-key") {
            """
                {
                    "colors": "LIGHT",
                    "unknown": true
                }
            """.trimIndent()
        }
        val settings = Settings.SettingsData(colors = KotifyColors.LIGHT)
        assertThat(Settings.load(tempFile)).isEqualTo(settings)
    }

    @Test
    fun loadInvalidJson() {
        val tempFile = createTempJsonFile("load-invalid-colors") { """abc""" }
        assertThat(Settings.load(tempFile)).isNull()
    }

    private fun createTempJsonFile(name: String, contents: (() -> String)? = null): File {
        val tempFile = File.createTempFile("kotify-$name", ".json", File("."))
        tempFile.deleteOnExit()
        if (contents != null) tempFile.writeText(contents())
        return tempFile
    }
}
