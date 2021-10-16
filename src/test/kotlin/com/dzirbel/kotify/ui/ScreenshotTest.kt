package com.dzirbel.kotify.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.TestComposeWindow
import com.dzirbel.kotify.ui.theme.Colors
import com.dzirbel.kotify.ui.theme.Theme
import java.io.File

private val resourcesDir = File("src/test/resources")
private val screenshotsDir = resourcesDir.resolve("screenshots")

/**
 * Runs a basic screenshot test for [content].
 *
 * If [record] is true, this will write a PNG file with the image displayed by [content]. Otherwise, this verifies that
 * the previously recorded screenshot matches exactly the image displayed by [content]. This is a simple mechanism to
 * generate reference screenshots when creating a test and verifying them otherwise. If [record] is true an
 * [AssertionError] will be thrown, failing the test, to ensure that [record] is not left enabled.
 *
 * Screenshots are saved in the test resources directory under a "screenshots" directory, then under a folder with the
 * fully qualified class name of the receiving object (i.e. the test class), and finally in a file with the given
 * [filename] (to allow multiple screenshots from the same test class).
 */
fun Any.screenshotTest(
    filename: String,
    windowWidth: Int = 1024,
    windowHeight: Int = 768,
    record: Boolean = false,
    colorsSet: Set<Colors> = Colors.values().toSet(),
    content: @Composable () -> Unit,
) {
    val multipleColorSets = colorsSet.size > 1
    var recordedScreenshots = false
    for (colors in colorsSet) {
        val window = TestComposeWindow(width = windowWidth, height = windowHeight)
        window.setContent {
            Theme.apply(colors = colors) {
                Box(Modifier.background(colors.surface3)) {
                    content()
                }
            }
        }

        val screenshotData = window.surface.makeImageSnapshot().encodeToData()
        requireNotNull(screenshotData) { "failed to encode screenshot to data" }
        val screenshotBytes = screenshotData.bytes

        val className = requireNotNull(this::class.qualifiedName) {
            "no class qualified name: screenshotTest() may not be called from local/anonymous classes"
        }
        val classScreenshotsDir = screenshotsDir.resolve(className)
        val filenameWithColors = if (multipleColorSets) "$filename-${colors.name.lowercase()}" else filename
        val screenshotFile = classScreenshotsDir.resolve("$filenameWithColors.png")

        if (record || !screenshotFile.exists()) {
            recordedScreenshots = true
            classScreenshotsDir.mkdirs()
            screenshotFile.writeBytes(screenshotBytes)
            println("Wrote screenshot $filename to $screenshotFile")
        } else {
            val recordedBytes = screenshotFile.readBytes()
            val mismatchFile = classScreenshotsDir.resolve("$filenameWithColors-MISMATCH.png")
            if (!screenshotBytes.contentEquals(recordedBytes)) {
                mismatchFile.writeBytes(screenshotBytes)
                throw AssertionError(
                    "Screenshot mismatch for $screenshotFile. The image generated in the test has been written to " +
                        "$mismatchFile for comparison (but then should be deleted)."
                )
            } else {
                mismatchFile.delete()
            }
        }
    }

    if (recordedScreenshots) {
        throw AssertionError(
            "Failing test because screenshots were recorded. Screenshots were successfully saved; this assertion " +
                "ensures that all live tests are not in record mode and all screenshots exist."
        )
    }
}
