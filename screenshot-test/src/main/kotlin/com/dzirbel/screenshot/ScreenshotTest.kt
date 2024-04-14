package com.dzirbel.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import java.io.File

private val resourcesDir = File("src/test/resources")
private val screenshotsDir = resourcesDir.resolve("screenshots")

private val regenScreenshots: Boolean
    get() = System.getenv("REGEN_SCREENSHOTS")?.toBoolean() == true

fun Any.screenshotTest(
    filename: String,
    windowWidth: Int = 1024,
    windowHeight: Int = 768,
    windowDensity: Density = Density(1f),
    record: Boolean = false,
    setUpComposeScene: ImageComposeScene.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    screenshotTest(
        filename = filename,
        configurations = listOf(Unit),
        windowWidth = windowWidth,
        windowHeight = windowHeight,
        windowDensity = windowDensity,
        record = record,
        setUpComposeScene = setUpComposeScene,
    ) {
        content()
    }
}

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
fun <T> Any.screenshotTest(
    filename: String,
    configurations: List<T>,
    windowWidth: Int = 1024,
    windowHeight: Int = 768,
    windowDensity: Density = Density(1f),
    record: Boolean = false,
    setUpComposeScene: ImageComposeScene.() -> Unit = {},
    configurationName: (T) -> String = { it.toString() },
    onConfiguration: (T) -> Unit = {},
    content: @Composable (T) -> Unit,
) {
    val mismatches = mutableListOf<Pair<File, File>>()
    var recordedScreenshots = false

    val className = requireNotNull(this::class.qualifiedName) {
        "no class qualified name: screenshotTest() may not be called from local/anonymous classes"
    }
    val classScreenshotsDir = screenshotsDir.resolve(className)

    for (configuration in configurations) {
        onConfiguration(configuration)

        // run in AWT thread as a workaround to https://github.com/JetBrains/compose-jb/issues/1691
        val screenshotImage = runBlocking(Dispatchers.Swing) {
            ImageComposeScene(width = windowWidth, height = windowHeight, density = windowDensity).use { scene ->
                scene.setContent {
                    content(configuration)
                }

                scene.setUpComposeScene()

                scene.render()
            }
        }

        val screenshotData = requireNotNull(screenshotImage.encodeToData()) { "failed to encode screenshot to data" }
        val screenshotBytes = screenshotData.bytes

        val filenameWithConfiguration =
            if (configurations.size > 1) "$filename-${configurationName(configuration)}" else filename
        val screenshotFile = classScreenshotsDir.resolve("$filenameWithConfiguration.png")

        if (record || regenScreenshots || !screenshotFile.exists()) {
            recordedScreenshots = true
            classScreenshotsDir.mkdirs()
            screenshotFile.writeBytes(screenshotBytes)

            @Suppress("ForbiddenMethodCall")
            println("Wrote screenshot $filename to $screenshotFile")
        } else {
            val recordedBytes = screenshotFile.readBytes()
            val mismatchFile = classScreenshotsDir.resolve("$filenameWithConfiguration-MISMATCH.png")
            if (!screenshotBytes.contentEquals(recordedBytes)) {
                mismatchFile.writeBytes(screenshotBytes)
                mismatches.add(screenshotFile to mismatchFile)
            } else {
                mismatchFile.delete()
            }
        }
    }

    if (mismatches.isNotEmpty()) {
        if (mismatches.size == 1) {
            val (screenshotFile, mismatchFile) = mismatches.first()
            throw AssertionError(
                "Screenshot mismatch for $screenshotFile. The image generated in the test has been written to " +
                    "$mismatchFile for comparison (but then should be deleted).",
            )
        }

        val message = buildString {
            appendLine("${mismatches.size} / ${configurations.size} mismatching screenshots:")
            for ((screenshotFile, mismatchFile) in mismatches) {
                appendLine("    $screenshotFile (the generated image has been written to $mismatchFile for comparison)")
            }
        }
        throw AssertionError(message)
    }

    if (recordedScreenshots && !regenScreenshots) {
        throw AssertionError(
            "Failing test because screenshots were recorded. Screenshots were successfully saved; this assertion " +
                "ensures that all live tests are not in record mode and all screenshots exist.",
        )
    }
}
