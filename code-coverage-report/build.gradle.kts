import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

plugins {
    id("jacoco-report-aggregation")

    // create check/test tasks by default and avoid strange dependency resolution errors
    kotlin("jvm") version libs.versions.kotlin
}

@Suppress("UnstableApiUsage")
reporting {
    reports {
        create<JacocoCoverageReport>("jacocoMergedUnitTestReport") {
            testSuiteName = "test"
            tasks.check.configure { finalizedBy(reportTask) }
        }

        create<JacocoCoverageReport>("jacocoMergedScreenshotTestReport") {
            testSuiteName = "screenshotTest"
            tasks.check.configure { finalizedBy(reportTask) }
        }

        create<JacocoCoverageReport>("jacocoMergedIntegrationTestReport") {
            testSuiteName = "integrationTest"
            val integrationTest by tasks.registering(Task::class)
            integrationTest.configure { finalizedBy(reportTask) }
        }

        withType<JacocoCoverageReport> {
            tasks.register<JacocoReportFixTask>(reportTask.name + "Fix")
                .get().configureFrom(reportTask) // on-demand configuration causes failures syncing gradle
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

    init {
        outputs.upToDateWhen { outputReportFile.exists() }
    }

    override fun getGroup() = "Verification"

    override fun getDescription() = "Fix invalid 0-line methods in JaCoCo reports"

    fun configureFrom(jacocoReport: TaskProvider<JacocoReport>) {
        val fixTask = this

        fixTask.dependsOn(jacocoReport)
        jacocoReport.configure { finalizedBy(fixTask) }

        fixTask.reportFile.set(jacocoReport.map { it.reports.xml.outputLocation.get() })

        // hack: we cannot have the output file of this task be the same as its input file due to a circular reference,
        // so we create a different file, then finalize the task with a new one that replaces the input report
        fixTask.finalizedBy(
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
        val contents: String
        val readMs = measureTimeMillis { contents = file.readText() }
        logger.info("Read ${contents.length} characters in ${readMs.milliseconds}")

        val fixed: String
        val replaceMs = measureTimeMillis { fixed = contents.replace(INVALID_METHOD, "") }
        val sizeDiff = contents.length - fixed.length
        logger.info("Replaced $sizeDiff characters in ${replaceMs.milliseconds}")

        if (sizeDiff > 0) {
            val outputFile = outputReportFile
            val writeMs = measureTimeMillis { outputFile.writeText(fixed) }
            logger.info("Wrote fixed contents to ${outputFile.relativeTo(rootDir).path} in ${writeMs.milliseconds}")

            val replacements = sizeDiff / INVALID_METHOD.length
            logger.warn("Replaced $replacements invalid line numbers in $filePath")
        } else {
            logger.warn("No 0-line methods found!")
        }

        val duration = (System.nanoTime() - start).nanoseconds
        logger.info("Done in $duration")
    }

    companion object {
        private const val INVALID_METHOD =
            """<method name="invokeSuspend" desc="(Ljava/lang/Object;)Ljava/lang/Object;" line="0">"""
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
