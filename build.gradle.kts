import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// apply plugins to the root project so that we can access their classes in the shared configuration
plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    id("jacoco")
    kotlin("jvm") version libs.versions.kotlin
}

subprojects {
    configurations.all {
        resolutionStrategy.failOnNonReproducibleResolution()
    }

    configureJacoco()
    configureTests()
    configureDetekt()
    configureKotlin()
}

fun Project.configureDetekt() {
    pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        detekt {
            source.from(project.files("src"))
            config.from(rootProject.files("detekt-config.yml"))
        }

        dependencies {
            detektPlugins(libs.detekt.formatting)
            detektPlugins(libs.detekt.compose)
        }

        // disable :detekt task since it does not run with type resolution; instead have it depend on all the other
        // tasks (detektMain, detektTest, etc.) which do, see https://detekt.dev/docs/gettingstarted/type-resolution
        tasks.detekt.configure {
            isEnabled = false
            dependsOn(tasks.withType<Detekt>().matching { it != this })
        }
    }
}

fun Project.configureKotlin() {
    pluginManager.withPlugin("kotlin") {
        kotlin {
            compilerOptions {
                allWarningsAsErrors = true
                jvmTarget = libs.versions.jvm.map(JvmTarget::fromTarget)

                // enable context receivers: https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md
                freeCompilerArgs.add("-Xcontext-receivers")

                freeCompilerArgs.add("-opt-in=kotlin.contracts.ExperimentalContracts")

                // hack: exclude runtime project because it has no coroutines dependency
                if (!project.name.contains("runtime")) {
                    freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
                    freeCompilerArgs.add("-opt-in=kotlinx.coroutines.DelicateCoroutinesApi") // allow use of GlobalScope
                }

                pluginManager.withPlugin("org.jetbrains.compose") {
                    // hack: exclude repository project because it only has the runtime dependency
                    if (!project.name.contains("repository")) {
                        freeCompilerArgs.add("-opt-in=androidx.compose.foundation.ExperimentalFoundationApi")
                        freeCompilerArgs.add("-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi") // allow use of FlowRow
                        freeCompilerArgs.add("-opt-in=androidx.compose.ui.ExperimentalComposeUiApi")
                    }

                    if (System.getenv("COMPOSE_METRICS")?.toBoolean() == true) {
                        // enable Compose compiler metrics and reports:
                        // https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md
                        val composeCompilerReportsDir = layout.buildDirectory.dir("compose").get()
                        val pluginPrefix = "plugin:androidx.compose.compiler.plugins.kotlin"
                        freeCompilerArgs.addAll("-P", "$pluginPrefix:metricsDestination=$composeCompilerReportsDir")
                        freeCompilerArgs.addAll("-P", "$pluginPrefix:reportsDestination=$composeCompilerReportsDir")
                    }
                }
            }
        }
    }

    pluginManager.withPlugin("java") {
        java {
            targetCompatibility = libs.versions.jvm.map { JavaVersion.valueOf("VERSION_$it") }.get()
        }
    }
}

fun Project.configureTests() {
    pluginManager.withPlugin("jvm-test-suite") {
        testing {
            @Suppress("UnstableApiUsage")
            suites {
                withType<JvmTestSuite> {
                    useJUnitJupiter()

                    dependencies {
                        implementation(project())
                        pluginManager.withPlugin("java-test-fixtures") {
                            implementation(testFixtures(project()))
                        }

                        implementation(libs.assertk)
                    }
                }

                pluginManager.withPlugin("org.jetbrains.compose") {
                    register<JvmTestSuite>("screenshotTest") {
                        testType = "screenshot-test"

                        dependencies {
                            implementation(project(":screenshot-test"))
                        }

                        targets {
                            all {
                                // consider screenshot tests up to date based on REGEN_SCREENSHOTS environment variable
                                testTask.configure {
                                    inputs.property("regen", System.getenv("REGEN_SCREENSHOTS")?.toBoolean() == true)
                                }

                                // default "test" task is finalized by the "screenshotTest" task
                                tasks.named { it == "test" }.configureEach {
                                    finalizedBy(testTask)
                                }
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

        // disable unused test reports
        reports.all { required = false }

        jvmArgs = listOf(
            // allowing mocking of java.time in JDK 16+ per https://mockk.io/doc/md/jdk16-access-exceptions.html
            "--add-opens",
            "java.base/java.time=ALL-UNNAMED",

            // avoid warning for mockk dynamic agent loading in Java 21+: https://github.com/mockito/mockito/issues/3037
            "-XX:+EnableDynamicAgentLoading",
        )

        // run tests in parallel and fork a new JVM for large test suites
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        forkEvery = 100

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
    pluginManager.withPlugin("jacoco") {
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
}
