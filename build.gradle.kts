
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

// TODO move common configuration to buildSrc
subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    afterEvaluate {
        configureKotlin()
        configureDetekt()
        configureTests()
        configureJacoco()

        configurations.all {
            resolutionStrategy {
                failOnNonReproducibleResolution()
            }
        }

        tasks.create<Task>("checkLocal") {
            dependsOn("detektWithTypeResolution")
            dependsOn("testLocal")
        }
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
            jvmTarget.set(libs.versions.jvm.map(JvmTarget::fromTarget))

            freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
            freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
            freeCompilerArgs.add("-opt-in=kotlinx.coroutines.FlowPreview")
            freeCompilerArgs.add("-opt-in=kotlin.contracts.ExperimentalContracts")
            freeCompilerArgs.add("-opt-in=kotlinx.coroutines.DelicateCoroutinesApi") // allow use of GlobalScope

            // enable context receivers: https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md
            freeCompilerArgs.add("-Xcontext-receivers")

            if (project.extensions.findByType<ComposeExtension>() != null) {
                freeCompilerArgs.add("-opt-in=androidx.compose.ui.ExperimentalComposeUiApi")
                freeCompilerArgs.add("-opt-in=androidx.compose.foundation.ExperimentalFoundationApi")
                freeCompilerArgs.add("-opt-in=androidx.compose.material.ExperimentalMaterialApi")

                // enable Compose compiler metrics and reports:
                // https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md
                val composeCompilerReportsDir = project.buildDir.resolve("compose")
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
}

fun Project.configureTests() {
    tasks.test {
        useJUnitPlatform()
    }

    tasks.create<Test>("testLocal") {
        useJUnitPlatform {
            excludeTags("network")
        }
    }

    tasks.create<Test>("testIntegration") {
        useJUnitPlatform {
            includeTags("network")
        }
    }

    tasks.withType<Test>().configureEach {
        systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }

        // hacky, but causes gradle builds to fail if tests write or std_err (which often indicates exceptions handled
        // by the Thread.uncaughtExceptionHandler)
        addTestOutputListener { _, outputEvent ->
            if (outputEvent.destination == TestOutputEvent.Destination.StdErr) {
                throw AssertionError("Failing due to test output to STANDARD_ERROR")
            }
        }
    }
}

val jacocoTestReportLocal = project.tasks.create<JacocoReport>("jacocoTestReportLocal")
val jacocoTestReportIntegration = project.tasks.create<JacocoReport>("jacocoTestReportIntegration")
configureJacoco() // configure jacoco for the root project to use correct version and report settings

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
            xml.required.set(true)
            csv.required.set(false)
            html.required.set(false)
        }
    }
}
