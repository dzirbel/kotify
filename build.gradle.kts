import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    kotlin("jvm") version deps.versions.kotlin.get()
    kotlin("plugin.serialization") version deps.versions.kotlin.get()

    alias(deps.plugins.detekt)
    alias(deps.plugins.compose)

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
    implementation(compose.desktop.currentOs)

    implementation(deps.okhttp)
    implementation(deps.ktor.netty)
    implementation(deps.coroutines.core)
    implementation(deps.kotlinx.serialization)
    implementation(deps.slf4j.nop)

    implementation(deps.bundles.exposed)
    implementation(deps.sqlite.jdbc)

    testImplementation(deps.coroutines.test)
    testImplementation(deps.bundles.junit5.api)
    testRuntimeOnly(deps.junit5.engine)

    // JUnit 4 is required to run Compose tests
    testCompileOnly(deps.junit4)
    testRuntimeOnly(deps.junit5.vintage.engine)
    testImplementation(deps.compose.test.junit4)

    testImplementation(deps.assertk)
    testImplementation(deps.ktor.client)
    testImplementation(deps.mockk)
    testImplementation(deps.compose.swing) // Swing dispatcher for screenshot tests

    testFixturesImplementation(deps.assertk)
    testFixturesImplementation(deps.bundles.exposed)
    testFixturesImplementation(compose.desktop.currentOs)
    testFixturesImplementation(deps.kotlinx.serialization) // TODO necessary for the opt-in
    testFixturesImplementation(deps.okhttp)
    testFixturesImplementation(deps.coroutines.core)

    detektPlugins(deps.detekt.formatting)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.allWarningsAsErrors = true
    kotlinOptions.jvmTarget = "16"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.time.ExperimentalTime"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.FlowPreview"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.contracts.ExperimentalContracts"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.DelicateCoroutinesApi" // allow use of GlobalScope
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=androidx.compose.ui.ExperimentalComposeUiApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=androidx.compose.material.ExperimentalMaterialApi"
}

configurations.all {
    resolutionStrategy {
        failOnNonReproducibleResolution()
    }
}

val testLocal = tasks.create<Test>("testLocal") {
    useJUnitPlatform {
        excludeTags("network")
    }
}

val testIntegration = tasks.create<Test>("testIntegration") {
    useJUnitPlatform {
        includeTags("network")
    }
}

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

jacoco {
    toolVersion = deps.versions.jacoco.get()
}

tasks.create<JacocoReport>("jacocoTestReportLocal") {
    dependsOn(testLocal)
    executionData(testLocal)
    sourceSets(sourceSets.main.get())
}

tasks.create<JacocoReport>("jacocoTestReportIntegration") {
    dependsOn(testIntegration)
    executionData(testIntegration)
    sourceSets(sourceSets.main.get())
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.create<Task>("checkLocal") {
    dependsOn("detekt")
    dependsOn("testLocal")
}

detekt {
    source = files("src")
    config = files("detekt-config.yml")
}

compose.desktop {
    application {
        // workaround for https://github.com/JetBrains/compose-jb/issues/188
        if (OperatingSystem.current().isLinux) {
            jvmArgs("-Dsun.java2d.uiScale=2.0")
        }

        mainClass = "com.dzirbel.kotify.MainKt"

        nativeDistributions {
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
