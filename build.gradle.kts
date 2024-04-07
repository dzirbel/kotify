
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import kotlin.system.measureTimeMillis
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

// apply plugins to the root project so that we can access their classes in the shared configuration
plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
}

// provide a repository for the root project to resolve jacoco
repositories {
    mavenCentral()
}

// create root project tasks which depend on all subproject test tasks
val jacocoTestReportLocal = project.tasks.create<JacocoReport>("jacocoTestReportLocal")
val jacocoTestReportIntegration = project.tasks.create<JacocoReport>("jacocoTestReportIntegration")

tasks.create<JacocoReportFixTask>("jacocoReportFixLocal") {
    configureFrom(jacocoTestReportLocal)
}

tasks.create<JacocoReportFixTask>("jacocoReportFixIntegration") {
    configureFrom(jacocoTestReportIntegration)
}

configureJacoco() // configure jacoco for the root project to use correct version and report settings

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }

    configurations.all {
        resolutionStrategy {
            failOnNonReproducibleResolution()
        }
    }

    tasks.create<Task>("checkLocal") {
        dependsOn("detektWithTypeResolution")
        dependsOn("testLocal")
    }

    afterEvaluate {
        configureKotlin()
        configureDetekt()
        configureTests()
        configureJacoco()
    }
}

fun Project.configureDetekt() {
    detekt {
        source.from(project.files("src"))
        config.from(rootProject.files("detekt-config.yml"))
    }

    dependencies {
        detektPlugins(libs.detekt.formatting)
        detektPlugins(libs.twitter.compose.rules)
    }

    val hasTestFixtures = tasks.findByName("detektTestFixtures") != null
    if (hasTestFixtures) {
        tasks.detektTestFixtures.configure {
            // enable type resolution for detekt on test fixtures
            jvmTarget = libs.versions.jvm.get()
            classpath.from(files("src/testFixtures"))
        }
    }

    // run with type resolution; see https://detekt.dev/docs/gettingstarted/type-resolution
    tasks.create("detektWithTypeResolution") {
        dependsOn(tasks.detektMain)
        dependsOn(tasks.detektTest)
        if (hasTestFixtures) {
            dependsOn(tasks.detektTestFixtures)
        }
    }
}

fun Project.configureKotlin() {
    kotlin {
        compilerOptions {
            allWarningsAsErrors = true
            jvmTarget = libs.versions.jvm.map(JvmTarget::fromTarget)

            // hack: exclude runtime project because it has no coroutines dependency
            if (!name.contains("runtime")) {
                freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
                freeCompilerArgs.add("-opt-in=kotlin.contracts.ExperimentalContracts")
                freeCompilerArgs.add("-opt-in=kotlinx.coroutines.DelicateCoroutinesApi") // allow use of GlobalScope
            }

            // enable context receivers: https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md
            freeCompilerArgs.add("-Xcontext-receivers")

            if (extensions.findByType<ComposeExtension>() != null) {
                // hack: exclude repository project because it only has the runtime dependency
                if (!name.contains("repository")) {
                    freeCompilerArgs.add("-opt-in=androidx.compose.ui.ExperimentalComposeUiApi")
                    freeCompilerArgs.add("-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi") // allow use of FlowRow
                    freeCompilerArgs.add("-opt-in=androidx.compose.foundation.ExperimentalFoundationApi")
                    freeCompilerArgs.add("-opt-in=androidx.compose.material.ExperimentalMaterialApi")
                }

                // enable Compose compiler metrics and reports:
                // https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md
                val composeCompilerReportsDir = layout.buildDirectory.dir("compose").get()
                freeCompilerArgs.addAll(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$composeCompilerReportsDir"
                )

                freeCompilerArgs.addAll(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$composeCompilerReportsDir"
                )
            }
        }
    }

    java {
        targetCompatibility = libs.versions.jvm.map { JavaVersion.valueOf("VERSION_$it") }.get()
    }
}

fun Project.configureTests() {
    tasks.test {
        useJUnitPlatform()
    }

    tasks.create<Test>("testLocal") {
        description = "Run non-network tests"

        useJUnitPlatform {
            excludeTags("network")
        }
    }

    tasks.create<Test>("testIntegration") {
        description = "Run network tests"

        useJUnitPlatform {
            includeTags("network")
        }
    }

    tasks.create<Test>("regenScreenshots") {
        description = "Run unit tests and regenerate screenshots"

        // ideally, only screenshot tests would be run, but they do not have their own tag
        useJUnitPlatform {
            excludeTags("network")
        }

        systemProperties("REGEN_SCREENSHOTS" to true)
    }

    tasks.withType<Test> {
        systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }

        // allowing mocking of java.time in JDK 16+ per https://mockk.io/doc/md/jdk16-access-exceptions.html
        jvmArgs = listOf("--add-opens", "java.base/java.time=ALL-UNNAMED")

        // hacky, but causes gradle builds to fail if tests write or std_err (which often indicates exceptions handled
        // by the Thread.uncaughtExceptionHandler)
        addTestOutputListener(
            object : TestOutputListener {
                private var hasFailed = false // only throw an AssertionError once to avoid them cluttering the output

                override fun onOutput(testDescriptor: TestDescriptor, outputEvent: TestOutputEvent) {
                    if (!hasFailed && outputEvent.destination == TestOutputEvent.Destination.StdErr) {
                        hasFailed = true
                        throw AssertionError("Failing due to test output to STANDARD_ERROR")
                    }
                }
            }
        )
    }
}

fun Project.configureJacoco() {
    jacoco {
        toolVersion = libs.versions.jacoco.get()
    }

    tasks.named { it == "testLocal" }.configureEach {
        jacocoTestReportLocal.dependsOn(this)
        jacocoTestReportLocal.sourceSets(sourceSets.main.get())
        jacocoTestReportLocal.executionData(this)
    }

    tasks.named { it == "testIntegration" }.configureEach {
        jacocoTestReportIntegration.dependsOn(this)
        jacocoTestReportIntegration.sourceSets(sourceSets.main.get())
        jacocoTestReportIntegration.executionData(this)
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required = true
            csv.required = false
            html.required = true
        }
    }
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
        val (replaceMs, fixed) = measureTimeMillisWithResult { contents.replace(INVALID_METHOD, "") }
        val sizeDiff = contents.length - fixed.length
        logger.info("Replaced $sizeDiff characters in ${replaceMs.milliseconds}")

        if (sizeDiff > 0) {
            val outputFile = outputReportFile
            val writeMs = measureTimeMillis { outputFile.writeText(fixed) }
            logger.info("Wrote fixed contents to ${outputFile.relativeTo(rootDir).path} in ${writeMs.milliseconds}")

            val replacements = sizeDiff / INVALID_METHOD.length
            logger.warn("Replaced $replacements invalid line numbers in $filePath")
        } else {
            logger.info("No 0-line methods found!")
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
