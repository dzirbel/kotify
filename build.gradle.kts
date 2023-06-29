
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    kotlin("jvm") version libs.versions.kotlin
    kotlin("plugin.serialization") version libs.versions.kotlin

    alias(libs.plugins.detekt)
    alias(libs.plugins.compose)

    `java-test-fixtures`

    jacoco
}

val appProperties = file("src/main/resources/app.properties").inputStream().use { Properties().apply { load(it) } }

version = appProperties["version"] as String

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project(":network"))

    implementation(compose.desktop.currentOs)

    implementation(libs.okhttp)
    implementation(libs.ktor.netty)
    implementation(libs.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.immutable.collections)
    implementation(libs.slf4j.nop)

    implementation(libs.bundles.exposed)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)

    // JUnit 4 is required to run Compose tests
    testCompileOnly(libs.junit4)
    testRuntimeOnly(libs.junit5.engine.vintage)
    testImplementation(libs.compose.junit4)

    testImplementation(libs.assertk)
    testImplementation(libs.ktor.client)
    testImplementation(libs.coroutines.swing) // Swing dispatcher for screenshot tests

    testFixturesImplementation(libs.assertk)
    testFixturesImplementation(libs.bundles.exposed)
    testFixturesImplementation(compose.desktop.currentOs)
    testFixturesImplementation(libs.kotlinx.serialization.json)
    testFixturesImplementation(libs.okhttp)
    testFixturesImplementation(libs.coroutines.core)
}

// TODO change to subprojects when no code remains in the root project
allprojects {
    // TODO move common configuration to buildSrc plugin
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
        source.from(files("src"))
        config.from(files("detekt-config.yml"))
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
            freeCompilerArgs += "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
            freeCompilerArgs += "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
            freeCompilerArgs += "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
            freeCompilerArgs += "-opt-in=androidx.compose.material.ExperimentalMaterialApi"

            // enable context receivers: https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md
            freeCompilerArgs += "-Xcontext-receivers"

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

    tasks.withType<JavaCompile>().configureEach {
        targetCompatibility = libs.versions.jvm.get()
    }
}

fun Project.configureTests() {
    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<Test>().configureEach {
        systemProperty("junit.jupiter.extensions.autodetection.enabled", true)
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
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
}

fun Project.configureJacoco() {
    jacoco {
        toolVersion = libs.versions.jacoco.get()
    }

    tasks.create<JacocoReport>("jacocoTestReportLocal") {
        dependsOn("testLocal")
        executionData("testLocal")
        sourceSets(sourceSets.main.get())
    }

    tasks.create<JacocoReport>("jacocoTestReportIntegration") {
        dependsOn("testIntegration")
        executionData("testIntegration")
        sourceSets(sourceSets.main.get())
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            csv.required.set(false)
        }
    }
}
