import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

private object Versions {
    const val coroutines = "1.5.2" // https://github.com/Kotlin/kotlinx.coroutines
    const val detekt = "1.18.1" // https://github.com/detekt/detekt; also update plugin version
    const val jacoco = "0.8.7" // https://github.com/jacoco/jacoco
    const val junit = "5.8.1" // https://junit.org/junit5/
    const val kotlinReflect = "1.5.31" // should match kotlin version
    const val kotlinxSerialization = "1.0.1" // https://github.com/Kotlin/kotlinx.serialization
    const val slf4j = "1.7.32" // http://www.slf4j.org/
    const val ktor = "1.6.4" // https://ktor.io/changelog/
    const val mockk = "1.12.0" // https://mockk.io/
    const val okhttp = "4.9.2" // https://square.github.io/okhttp/
    const val truth = "1.1.3" // https://truth.dev/
}

plugins {
    // https://kotlinlang.org/releases.html
    kotlin("jvm") version "1.5.31"

    // https://github.com/Kotlin/kotlinx.serialization
    kotlin("plugin.serialization") version "1.5.31"

    // https://github.com/detekt/detekt; also update dependency version
    id("io.gitlab.arturbosch.detekt") version "1.18.1"

    // https://plugins.gradle.org/plugin/name.remal.check-dependency-updates
    id("name.remal.check-dependency-updates") version "1.5.0"

    // https://github.com/jetbrains/compose-jb
    id("org.jetbrains.compose") version "1.0.0-alpha4-build398"

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

    implementation("com.squareup.okhttp3", "okhttp", Versions.okhttp)
    implementation("io.ktor", "ktor-server-netty", Versions.ktor)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Versions.coroutines)
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", Versions.kotlinxSerialization)
    implementation("org.slf4j", "slf4j-nop", Versions.slf4j)

    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", Versions.coroutines)
    testImplementation("org.junit.jupiter", "junit-jupiter-api", Versions.junit)
    testImplementation("org.junit.jupiter", "junit-jupiter-params", Versions.junit)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", Versions.junit)

    // JUnit 4 is required to run Compose tests
    testCompileOnly("junit:junit:4.13")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    testImplementation(compose("org.jetbrains.compose.ui:ui-test-junit4"))

    testImplementation("com.google.truth", "truth", Versions.truth)
    testImplementation("io.mockk", "mockk", Versions.mockk)

    testFixturesImplementation(compose.desktop.currentOs)
    testFixturesImplementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", Versions.kotlinxSerialization) // TODO necessary for the opt-in
    testFixturesImplementation("com.squareup.okhttp3", "okhttp", Versions.okhttp)
    testFixturesImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Versions.coroutines)
    testFixturesImplementation("com.google.truth", "truth", Versions.truth)

    detektPlugins("io.gitlab.arturbosch.detekt", "detekt-formatting", Versions.detekt)
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
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.InternalSerializationApi"
    kotlinOptions.freeCompilerArgs += "-Xopt-in=androidx.compose.ui.ExperimentalComposeUiApi"
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
    finalizedBy("jacocoTestReportLocal")
}

val testIntegration = tasks.create<Test>("testIntegration") {
    useJUnitPlatform {
        includeTags("network")
    }
    finalizedBy("jacocoTestReportIntegration")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<Test>().configureEach {
    testLogging {
        events = setOf(TestLogEvent.FAILED, TestLogEvent.STANDARD_ERROR, TestLogEvent.STANDARD_OUT)
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

jacoco {
    toolVersion = Versions.jacoco
}

tasks.create<JacocoReport>("jacocoTestReportLocal") {
    executionData(testLocal)
    sourceSets(sourceSets.main.get())
}

tasks.create<JacocoReport>("jacocoTestReportIntegration") {
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
