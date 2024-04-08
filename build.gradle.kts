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

subprojects {
    configurations.all {
        resolutionStrategy {
            failOnNonReproducibleResolution()
        }
    }

    // TODO refactor to use pluginManager.withPlugin
    afterEvaluate {
        configureJacoco()

        if (name != "code-coverage-report") {
            configureKotlin()
            configureDetekt()
            configureTests()
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
        // TODO does this duplicate work by also depending on the base detekt task?
        tasks.check.get().dependsOn(this)

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
    @Suppress("UnstableApiUsage")
    testing {
        suites {
            withType<JvmTestSuite> {
                useJUnitJupiter()

                dependencies {
                    implementation(project())

                    if (project.plugins.any { it is JavaTestFixturesPlugin }) {
                        implementation(testFixtures(project()))
                    }

                    implementation(libs.assertk)
                }
            }

            if (extensions.findByType<ComposeExtension>() != null) {
                register<JvmTestSuite>("screenshotTest") {
                    testType = "screenshot-test"

                    targets {
                        all {
                            // consider screenshot tests up to date based on REGEN_SCREENSHOTS environment variable
                            testTask.configure {
                                inputs.property("regen", System.getenv("REGEN_SCREENSHOTS")?.toBoolean() == true)
                            }

                            // default "test" task is finalized by the "screenshotTest" task
                            tasks.test.configure {
                                finalizedBy(testTask)
                            }
                        }
                    }
                }
            }
        }
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

    tasks.withType<JacocoReport> {
        reports {
            xml.required = true
            csv.required = false
            html.required = true
        }
    }
}
