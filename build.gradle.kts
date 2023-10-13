import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

configureJacoco() // configure jacoco for the root project to use correct version and report settings

subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
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

    tasks.withType<Test>().configureEach {
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

    tasks.findByName("testLocal")?.let { testLocal ->
        jacocoTestReportLocal.dependsOn(testLocal)
        jacocoTestReportLocal.sourceSets(sourceSets.main.get())
        jacocoTestReportLocal.executionData(testLocal)
    }

    tasks.findByName("testIntegration")?.let { testIntegration ->
        jacocoTestReportIntegration.dependsOn(testIntegration)
        jacocoTestReportIntegration.sourceSets(sourceSets.main.get())
        jacocoTestReportIntegration.executionData(testIntegration)
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required = true
            csv.required = false
            html.required = true
        }
    }
}
