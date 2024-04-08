import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

plugins {
    id("jacoco-report-aggregation")

    // add to create check/test tasks by default and avoid strange dependency resolution errors
    kotlin("jvm") version libs.versions.kotlin
}

val integrationTest = tasks.create("integrationTest")

@Suppress("UnstableApiUsage")
reporting {
    reports {
        create<JacocoCoverageReport>("jacocoMergedUnitTestReport") {
            testType = TestSuiteType.UNIT_TEST
            tasks.check.configure { finalizedBy(reportTask) }
        }

        create<JacocoCoverageReport>("jacocoMergedScreenshotTestReport") {
            testType = "screenshot-test"
            tasks.check.configure { finalizedBy(reportTask) }
        }

        create<JacocoCoverageReport>("jacocoMergedIntegrationTestReport") {
            testType = TestSuiteType.INTEGRATION_TEST
            integrationTest.finalizedBy(reportTask)
        }

        withType<JacocoCoverageReport> {
            tasks.create<JacocoReportFixTask>(reportTask.name + "Fix") {
                configureFrom(reportTask.get())
            }
        }
    }
}

dependencies {
    jacocoAggregation(project(":application")) // adds other modules transitively
}

/**
 * Hack to work around an issue where JaCoCo reports include synthetic methods at line 0, which causes codecov
 * processing to fail. Instead of fixing the root cause, this task runs on JaCoCo reports and removes any such methods.
 *
 * See:
 * - https://github.com/codecov/feedback/issues/72
 * - https://github.com/jacoco/jacoco/issues/1521
 * - https://github.com/Kotlin/kotlinx.coroutines/issues/3911
 */
abstract class JacocoReportFixTask : DefaultTask() {
    @get:InputFile
    abstract val reportFile: RegularFileProperty

    @get:OutputFile
    val outputReportFile: File
        get() = reportFile.get().asFile.run { resolveSibling(nameWithoutExtension + "Fixed." + extension) }

    // cannot access project during execution time
    private val rootDir = project.rootDir

    override fun getGroup() = "Verification"

    override fun getDescription() = "Fix invalid 0-line methods in JaCoCo reports"

    fun configureFrom(jacocoReport: JacocoReport) {
        reportFile.set(jacocoReport.reports.xml.outputLocation.get())

        this.dependsOn(jacocoReport)
        jacocoReport.finalizedBy(this)

        // hack: we cannot have the output file of this task be the same as its input file due to a circular reference,
        // so we create a different file, then finalize the task with a new one that replaces the input report
        this.finalizedBy(
            project.tasks.register<MoveFileTask>(name + "ReplaceReport") {
                src.set(outputReportFile)
                dst.set(reportFile)
            }
        )
    }

    @TaskAction
    fun run() {
        val start = System.nanoTime()

        val file = reportFile.get().asFile
        val filePath = file.relativeTo(rootDir).path
        logger.info("Checking $filePath...")

        // hack: read and write the entire file contents at once, without buffering/etc (note the report file does not
        // generally have newlines etc so parsing incrementally is non-trivial)
        val (readMs, contents) = measureTimeMillisWithResult { file.readText() }
        logger.info("Read ${contents.length} characters in ${readMs.milliseconds}")
        // add space to be able to find the number of changes
        val (replaceMs, fixed) = measureTimeMillisWithResult { contents.replace("line=\"0\"", " line=\"1\"") }
        val replacements = fixed.length - contents.length
        logger.info("Replaced $replacements 0-line references in ${replaceMs.milliseconds}")

        if (contents != fixed) {
            val outputFile = outputReportFile
            val writeMs = measureTimeMillis { outputFile.writeText(fixed) }
            logger.info("Wrote fixed contents to ${outputFile.relativeTo(rootDir).path} in ${writeMs.milliseconds}")

            logger.warn("Replaced $replacements invalid line numbers in $filePath")
        } else {
            logger.warn("No 0-line methods found!")
        }

        val duration = (System.nanoTime() - start).nanoseconds
        logger.info("Done in $duration")
    }
}

abstract class MoveFileTask : DefaultTask() {
    @get:InputFile
    abstract val src: RegularFileProperty

    @get:OutputFile
    abstract val dst: RegularFileProperty

    @TaskAction
    fun run() {
        val srcFile = src.get().asFile
        val dstFile = dst.get().asFile

        val durationMs = measureTimeMillis {
            dstFile.delete()
            srcFile.parentFile.mkdirs()
            srcFile.renameTo(dstFile)
        }
        logger.info("Moved ${srcFile.path} to ${dstFile.path} in ${durationMs.milliseconds}")
    }
}
