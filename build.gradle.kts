import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.detekt)
    id("jacoco")
    id("java-test-fixtures")
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin
}

val appProperties = file("src/main/resources/app.properties").inputStream().use { Properties().apply { load(it) } }

version = appProperties["version"] as String

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project(":db"))
    implementation(project(":network"))
    implementation(project(":repository"))

    testImplementation(testFixtures(project(":network")))

    testFixturesImplementation(project(":db"))
    testFixturesImplementation(project(":network"))
    testFixturesImplementation(testFixtures(project(":network")))

    implementation(compose.desktop.currentOs)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.javatime)
    implementation(libs.exposed.jdbc)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    // JUnit 4 is required to run Compose tests
    testCompileOnly(libs.junit4)
    testRuntimeOnly(libs.junit5.engine.vintage)
    testImplementation(libs.compose.junit4)

    testImplementation(libs.assertk)
    testImplementation(libs.kotlinx.coroutines.swing) // Swing dispatcher for screenshot tests
    testImplementation(libs.kotlinx.coroutines.test)

    testFixturesImplementation(compose.desktop.currentOs)
    testFixturesImplementation(libs.assertk)
    testFixturesImplementation(libs.exposed.core)
    testFixturesImplementation(libs.exposed.dao)
    testFixturesImplementation(libs.exposed.javatime)
    testFixturesImplementation(libs.exposed.jdbc)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.serialization.json)
    testFixturesImplementation(libs.okhttp)
}

val jacocoTestReportLocal = project.tasks.create<JacocoReport>("jacocoTestReportLocal")
val jacocoTestReportIntegration = project.tasks.create<JacocoReport>("jacocoTestReportIntegration")

// TODO change to subprojects when no code remains in the root project and/or move common configuration to buildSrc
allprojects {
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

compose.desktop {
    application {
        // workaround for https://github.com/JetBrains/compose-jb/issues/188
        if (OperatingSystem.current().isLinux) {
            jvmArgs("-Dsun.java2d.uiScale=2.0")
        }

        mainClass = "com.dzirbel.kotify.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules("java.sql")
            modules("jdk.crypto.ec") // required for SSL, see https://github.com/JetBrains/compose-jb/issues/429

            targetFormats(TargetFormat.Deb, TargetFormat.Exe)
            packageName = appProperties["name"] as String
            packageVersion = project.version.toString()
        }
    }
}

// override compose configuration of arguments so that they're only applied to :run and not when packaging the
// application
project.afterEvaluate {
    tasks.withType<JavaExec> {
        args = listOf(".kotify/cache", ".kotify/settings")
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
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            allWarningsAsErrors = true
            jvmTarget = libs.versions.jvm.get()

            freeCompilerArgs += "-opt-in=kotlin.time.ExperimentalTime"
            freeCompilerArgs += "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
            freeCompilerArgs += "-opt-in=kotlinx.coroutines.FlowPreview"
            freeCompilerArgs += "-opt-in=kotlin.contracts.ExperimentalContracts"
            freeCompilerArgs += "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi" // allow use of GlobalScope

            // enable context receivers: https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md
            freeCompilerArgs += "-Xcontext-receivers"

            if (project.extensions.findByType<ComposeExtension>() != null) {
                freeCompilerArgs += "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
                freeCompilerArgs += "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
                freeCompilerArgs += "-opt-in=androidx.compose.material.ExperimentalMaterialApi"

                // enable Compose compiler metrics and reports:
                // https://github.com/androidx/androidx/blob/androidx-main/compose/compiler/design/compiler-metrics.md
                val composeCompilerReportsDir = project.buildDir.resolve("compose")
                freeCompilerArgs += listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$composeCompilerReportsDir"
                )

                freeCompilerArgs += listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$composeCompilerReportsDir"
                )
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        targetCompatibility = libs.versions.jvm.get()
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
    }
}

fun Project.configureJacoco() {
    jacoco {
        toolVersion = libs.versions.jacoco.get()
    }

    val testLocal = tasks.getByName("testLocal")
    jacocoTestReportLocal.dependsOn(testLocal)
    jacocoTestReportLocal.sourceSets(sourceSets.main.get())
    jacocoTestReportLocal.executionData(testLocal)

    val testIntegration = tasks.getByName("testIntegration")
    jacocoTestReportIntegration.dependsOn(testIntegration)
    jacocoTestReportIntegration.sourceSets(sourceSets.main.get())
    jacocoTestReportIntegration.executionData(testIntegration)

    tasks.withType<JacocoReport> {
        reports {
            xml.required = true
            csv.required = false
            html.required = false
        }
    }
}
